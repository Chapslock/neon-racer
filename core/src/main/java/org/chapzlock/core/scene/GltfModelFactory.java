package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.animation.Channel;
import org.chapzlock.core.animation.Interpolation;
import org.chapzlock.core.animation.TargetPath;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.gltf.GltfAnimationData;
import org.chapzlock.core.asset.gltf.GltfAsset;
import org.chapzlock.core.asset.gltf.GltfMaterialData;
import org.chapzlock.core.asset.gltf.GltfMeshData;
import org.chapzlock.core.asset.gltf.GltfNodeData;
import org.chapzlock.core.asset.gltf.GltfSkinData;
import org.chapzlock.core.component.AlphaMode;
import org.chapzlock.core.component.AnimationClipSet;
import org.chapzlock.core.component.AnimationState;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.Name;
import org.chapzlock.core.component.OpaqueLayer;
import org.chapzlock.core.component.PbrMaterial;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Skeleton;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.render.MeshRegistry;
import org.chapzlock.core.render.PbrShaderProps;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureRegistry;

/**
 * Converts a parsed glTF asset into live ECS entities.
 *
 * <p><strong>What this does:</strong> Takes the CPU-side data from {@link GltfAsset}
 * (nodes, meshes, materials) and creates actual entities in the ECS with all the
 * components needed for rendering and gameplay. Think of it as "spawning" the 3D
 * model into the world.
 *
 * <p><strong>How it works:</strong>
 * <ol>
 *   <li>For each glTF node: create an entity with Transform, Hierarchy</li>
 *   <li>If the node has a mesh: upload to GPU via MeshRegistry, add Mesh + PbrMaterial + OpaqueLayer</li>
 *   <li>Wire up parent-child relationships in Hierarchy components</li>
 *   <li>Track named nodes for later lookup via GltfInstance</li>
 * </ol>
 *
 * <p><strong>When to use:</strong> Called by SceneManager when loading a glTF scene
 * or instantiating a prefab. Game code doesn't call this directly.
 *
 * <p><strong>Five-phase approach:</strong> The instantiation process is split into five
 * phases to ensure all entities exist before hierarchy links are created, and all hierarchy
 * is resolved before meshes reference parent entities:
 * <ol>
 *   <li><em>Phase 1:</em> Create all entities and set their local transforms (position,
 *       rotation, scale from the glTF node data).</li>
 *   <li><em>Phase 2:</em> Wire up parent-child hierarchy relationships now that all entity
 *       IDs are known.</li>
 *   <li><em>Phase 3:</em> Upload mesh geometry to the GPU and create PBR material components
 *       for nodes that reference meshes.</li>
 *   <li><em>Phase 4:</em> Create Skeleton components for skinned nodes, mapping joint node
 *       indices to entity IDs for GPU skinning.</li>
 *   <li><em>Phase 5:</em> Create Animation components from glTF animation data, remapping
 *       node indices to entity IDs and attaching an AnimationClipSet to the root entity.</li>
 * </ol>
 *
 * @see GltfAsset
 * @see GltfInstance
 */
public class GltfModelFactory {

    private final ComponentRegistry registry;
    private final MeshRegistry meshRegistry;
    private final TextureRegistry textureRegistry;
    private final ShaderSystem shaderSystem;

    /**
     * Creates an instantiator connected to the engine's subsystems.
     *
     * <p>The engine context provides access to all GPU systems needed for uploading
     * mesh data and compiling shaders. In unit tests, the GPU systems may be null —
     * the instantiator will skip mesh/material creation for nodes without meshes.
     *
     * @param ctx the engine context providing access to all subsystems
     */
    public GltfModelFactory(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.meshRegistry = ctx.getMeshRegistry();
        this.textureRegistry = ctx.getTextureRegistry();
        this.shaderSystem = ctx.getShaderSystem();
    }

    /**
     * Instantiates all nodes from a glTF asset as ECS entities.
     *
     * <p>This is the main entry point. It processes the entire glTF node tree and
     * produces a {@link GltfInstance} that maps node names to entity IDs for runtime
     * lookup by gameplay code.
     *
     * <p><strong>What happens for each node:</strong>
     * <ul>
     *   <li>An entity is created via {@link Entity#create(ComponentRegistry)}</li>
     *   <li>A {@link Transform} is set from the node's translation, rotation, and scale</li>
     *   <li>World-space transform data is stored directly in the Transform component
     *       (computed later by TransformSystem)</li>
     *   <li>A {@link Hierarchy} links it to its parent (or marks it as root)</li>
     *   <li>If the node has a mesh: {@link Mesh}, {@link PbrMaterial}, and {@link OpaqueLayer}
     *       are added for rendering</li>
     * </ul>
     *
     * @param asset the parsed glTF data containing nodes, meshes, and materials
     * @return a GltfInstance handle for name-based entity lookup
     */
    public GltfInstance instantiate(GltfAsset asset) {
        List<GltfNodeData> nodes = asset.nodes();
        int[] entityIds = new int[nodes.size()];
        Map<String, Integer> nameMap = new HashMap<>();

        // Phase 1: Create all entities and set transforms.
        // We create ALL entities first so that Phase 2 can reference any entity by index
        // when building hierarchy links (a child might appear before its parent in the list).
        for (int i = 0; i < nodes.size(); i++) {
            GltfNodeData node = nodes.get(i);
            int entityId = Entity.create(registry);
            entityIds[i] = entityId;

            // Set up local transform from glTF node TRS (Translation, Rotation, Scale)
            Transform transform = new Transform();
            transform.setPosition(node.translation());
            transform.setRotationQuaternion(node.rotation());
            transform.setScale(node.scale().x, node.scale().y, node.scale().z);

            registry.addComponent(entityId, transform);

            // Track named nodes for both GltfInstance lookup and registry name-based lookup
            if (node.name() != null && !node.name().isEmpty()) {
                nameMap.put(node.name(), entityId);
                registry.addComponent(entityId, new Name(node.name()));
            }
        }

        // Phase 2: Set up hierarchy relationships.
        // Now that all entities exist, we can safely reference parent/child entity IDs.
        for (int i = 0; i < nodes.size(); i++) {
            GltfNodeData node = nodes.get(i);
            List<Integer> childIndices = node.childIndices();

            // Determine if this is a root node (no parent) or a child node
            boolean isRoot = asset.rootNodeIndices().contains(i);
            Hierarchy hierarchy = isRoot
                ? Hierarchy.root()
                : findParentAndCreateHierarchy(i, nodes, entityIds);

            // Register all children in this node's hierarchy
            for (int childIdx : childIndices) {
                hierarchy.addChild(entityIds[childIdx]);
            }

            registry.addComponent(entityIds[i], hierarchy);
        }

        // Phase 3: Upload meshes and create materials.
        // Only runs for nodes that reference a mesh (meshIndex >= 0).
        // Skipped entirely if GPU systems are null (unit test mode).
        if (meshRegistry != null && shaderSystem != null) {
            Shader pbrShader = getOrCreatePbrShader();
            instantiateMeshes(asset, nodes, entityIds, pbrShader);
        }

        // Phase 4: Create Skeleton components for skinned nodes.
        instantiateSkeletons(asset, nodes, entityIds);

        // Determine root entity ID: use the first root node from the asset,
        // or fall back to the first entity if no roots are specified.
        int rootEntityId = asset.rootNodeIndices().isEmpty()
            ? entityIds[0]
            : entityIds[asset.rootNodeIndices().get(0)];

        // Phase 5: Create animations from glTF animation data.
        instantiateAnimations(asset, entityIds, rootEntityId);

        return new GltfInstance(rootEntityId, nameMap);
    }

    /**
     * Instantiates a glTF asset as children of an existing entity.
     *
     * <p>This is used for the {@code .gltfModel()} scene builder method, where a model
     * is loaded as part of a programmatic entity. The glTF nodes become children of the
     * specified parent entity instead of being standalone roots.
     *
     * <p><strong>How it differs from {@link #instantiate(GltfAsset)}:</strong>
     * <ul>
     *   <li>Root nodes get {@code Hierarchy.childOf(parentEntityId)} instead of {@code Hierarchy.root()}</li>
     *   <li>The parent entity's Hierarchy component gets the root children added to it</li>
     *   <li>AnimationClipSet and AnimationState are attached to the parent entity
     *       (not the glTF root node), so game code can control animations from the owning entity</li>
     * </ul>
     *
     * <p><strong>Same 5-phase logic as {@link #instantiate(GltfAsset)}:</strong>
     * <ol>
     *   <li><em>Phase 1:</em> Create entities with local transforms</li>
     *   <li><em>Phase 2:</em> Wire hierarchy — root nodes become children of parentEntityId</li>
     *   <li><em>Phase 3:</em> Upload meshes and create PBR materials</li>
     *   <li><em>Phase 4:</em> Create Skeleton components for skinned nodes</li>
     *   <li><em>Phase 5:</em> Create Animations — attached to the parent entity</li>
     * </ol>
     *
     * <p><strong>When to use:</strong> Called by SceneManager when a blueprint has a
     * {@code gltfModelPath} set via the builder's {@code .gltfModel()} method. Game code
     * does not call this directly.
     *
     * @param asset          the parsed glTF data containing nodes, meshes, materials, etc.
     * @param parentEntityId the entity to parent the model under — this entity becomes
     *                       the owner of the entire glTF node tree
     * @return a GltfInstance handle for name-based entity lookup within the instantiated model
     */
    public GltfInstance instantiateAsChild(GltfAsset asset, int parentEntityId) {
        List<GltfNodeData> nodes = asset.nodes();
        int[] entityIds = new int[nodes.size()];
        Map<String, Integer> nameMap = new HashMap<>();

        // Phase 1: Create all entities and set transforms.
        // Same as instantiate() — we need all entity IDs before wiring hierarchy.
        for (int i = 0; i < nodes.size(); i++) {
            GltfNodeData node = nodes.get(i);
            int entityId = Entity.create(registry);
            entityIds[i] = entityId;

            Transform transform = new Transform();
            transform.setPosition(node.translation());
            transform.setRotationQuaternion(node.rotation());
            transform.setScale(node.scale().x, node.scale().y, node.scale().z);

            registry.addComponent(entityId, transform);

            // Track named nodes for both GltfInstance lookup and registry name-based lookup
            if (node.name() != null && !node.name().isEmpty()) {
                nameMap.put(node.name(), entityId);
                registry.addComponent(entityId, new Name(node.name()));
            }
        }

        // Phase 2: Hierarchy — root nodes become children of parentEntityId.
        // Non-root nodes get their normal parent from the glTF tree.
        for (int i = 0; i < nodes.size(); i++) {
            GltfNodeData node = nodes.get(i);
            List<Integer> childIndices = node.childIndices();

            boolean isRoot = asset.rootNodeIndices().contains(i);
            Hierarchy hierarchy;
            if (isRoot) {
                // Key difference from instantiate(): root nodes are parented under the
                // provided entity instead of being free-standing roots.
                hierarchy = Hierarchy.childOf(parentEntityId);
            } else {
                hierarchy = findParentAndCreateHierarchy(i, nodes, entityIds);
            }

            for (int childIdx : childIndices) {
                hierarchy.addChild(entityIds[childIdx]);
            }
            registry.addComponent(entityIds[i], hierarchy);
        }

        // Register root entities as children in the parent's hierarchy component.
        // If the parent doesn't have a Hierarchy yet, create one as a root.
        Hierarchy parentHierarchy = registry.getComponent(parentEntityId, Hierarchy.class);
        if (parentHierarchy == null) {
            parentHierarchy = Hierarchy.root();
            registry.addComponent(parentEntityId, parentHierarchy);
        }
        for (int rootIdx : asset.rootNodeIndices()) {
            parentHierarchy.addChild(entityIds[rootIdx]);
        }

        // Phase 3: Meshes — same as instantiate()
        if (meshRegistry != null && shaderSystem != null) {
            Shader pbrShader = getOrCreatePbrShader();
            instantiateMeshes(asset, nodes, entityIds, pbrShader);
        }

        // Phase 4: Skeletons — same as instantiate()
        instantiateSkeletons(asset, nodes, entityIds);

        // Phase 5: Animations — key difference: attach to parentEntityId (not glTF root).
        // This lets game code find AnimationClipSet on the owning entity directly.
        instantiateAnimations(asset, entityIds, parentEntityId);

        int rootEntityId = asset.rootNodeIndices().isEmpty()
            ? entityIds[0]
            : entityIds[asset.rootNodeIndices().get(0)];

        return new GltfInstance(rootEntityId, nameMap);
    }

    /**
     * Uploads mesh data and creates PBR materials for all nodes that reference meshes.
     *
     * <p>A single glTF mesh can contain multiple "primitives" (sub-meshes), each with
     * its own material. The first primitive is attached to the node's own entity. Additional
     * primitives get their own child entities (with identity transforms) so each can have
     * its own material.
     *
     * @param asset     the glTF asset with mesh and material data
     * @param nodes     the flat list of glTF nodes
     * @param entityIds mapping from node index to entity ID
     * @param pbrShader the compiled PBR shader to use for all materials
     */
    private void instantiateMeshes(GltfAsset asset, List<GltfNodeData> nodes, int[] entityIds, Shader pbrShader) {
        for (int i = 0; i < nodes.size(); i++) {
            GltfNodeData node = nodes.get(i);
            if (node.meshIndex() < 0) continue;

            GltfMeshData meshData = asset.meshes().get(node.meshIndex());
            List<GltfMeshData.Primitive> primitives = meshData.primitives();

            for (int primIdx = 0; primIdx < primitives.size(); primIdx++) {
                GltfMeshData.Primitive primitive = primitives.get(primIdx);

                // Upload vertex data (positions, normals, UVs, indices) to GPU buffers
                Mesh mesh = meshRegistry.load(primitive.meshData());

                // Resolve material: use the referenced material or a default white one
                GltfMaterialData matData = primitive.materialIndex() >= 0
                    ? asset.materials().get(primitive.materialIndex())
                    : GltfMaterialData.defaultMaterial();

                PbrMaterial pbrMaterial = buildPbrMaterial(pbrShader, matData);

                if (primIdx == 0) {
                    // First primitive goes on the node's own entity
                    registry.addComponents(entityIds[i], mesh, pbrMaterial, new OpaqueLayer());
                } else {
                    // Additional primitives get their own child entities.
                    // This is necessary because each entity can only have one Mesh component,
                    // but a glTF mesh can have multiple sub-meshes with different materials.
                    int subEntity = Entity.create(registry);
                    Transform subTransform = new Transform(); // identity (same position as parent)
                    Hierarchy subHierarchy = Hierarchy.childOf(entityIds[i]);
                    registry.addComponents(subEntity, subTransform, subHierarchy, mesh, pbrMaterial, new OpaqueLayer());

                    // Also register this sub-entity as a child in the parent's hierarchy
                    Hierarchy parentHierarchy = registry.getComponent(entityIds[i], Hierarchy.class);
                    if (parentHierarchy != null) {
                        parentHierarchy.addChild(subEntity);
                    }
                }
            }
        }
    }

    /**
     * Finds which node is the parent of the given node index and creates the appropriate Hierarchy.
     *
     * <p>This performs a linear scan through all nodes looking for one whose {@code childIndices}
     * contains the target index. In typical glTF files with reasonable node counts (< 1000),
     * this O(n*m) approach is acceptable during one-time instantiation.
     *
     * @param nodeIndex the index of the node to find the parent for
     * @param nodes     the complete list of nodes in the asset
     * @param entityIds mapping from node index to entity ID
     * @return a Hierarchy configured with the correct parent, or a root hierarchy if no parent found
     */
    private Hierarchy findParentAndCreateHierarchy(int nodeIndex, List<GltfNodeData> nodes, int[] entityIds) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).childIndices().contains(nodeIndex)) {
                return Hierarchy.childOf(entityIds[i]);
            }
        }
        // No parent found in any node's childIndices — treat as root.
        // This can happen if rootNodeIndices doesn't list this node but it also isn't
        // referenced as a child anywhere (malformed data, defensive handling).
        return Hierarchy.root();
    }

    /**
     * Gets or creates the shared PBR shader instance.
     *
     * <p>The PBR shader implements Cook-Torrance BRDF with metallic-roughness workflow.
     * It's shared across all PBR materials in the scene to minimize shader switches.
     *
     * @return a compiled Shader component ready for use in materials
     */
    private Shader getOrCreatePbrShader() {
        Shader shader = new Shader(PbrShaderProps.VERTEX_FILE, PbrShaderProps.FRAGMENT_FILE);
        shaderSystem.compileAndLink(shader);
        return shader;
    }

    /**
     * Converts glTF material data into a PbrMaterial component.
     *
     * <p>Maps all glTF PBR properties (factors, textures, alpha mode) to the engine's
     * PbrMaterial builder. Textures are loaded via TextureRegistry, dispatching on whether
     * the texture is file-based (external URI) or memory-based (embedded in GLB).
     * If a texture reference is null, that texture slot is left empty and the shader will use
     * the scalar/vector factor uniformly.
     *
     * @param shader the compiled PBR shader to attach to this material
     * @param data   the parsed material properties from the glTF file
     * @return a fully configured PbrMaterial component
     */
    private PbrMaterial buildPbrMaterial(Shader shader, GltfMaterialData data) {
        var builder = PbrMaterial.builder()
            .shader(shader)
            .baseColorFactor(data.baseColorFactor())
            .metallicFactor(data.metallicFactor())
            .roughnessFactor(data.roughnessFactor())
            .emissiveFactor(data.emissiveFactor())
            .alphaCutoff(data.alphaCutoff())
            .doubleSided(data.doubleSided());

        builder.alphaMode(switch (data.alphaMode()) {
            case "MASK" -> AlphaMode.MASK;
            case "BLEND" -> AlphaMode.BLEND;
            default -> AlphaMode.OPAQUE;
        });

        if (textureRegistry != null) {
            loadTextureRef(data.baseColorTexture(), builder::baseColorMap, true);
            loadTextureRef(data.metallicRoughnessTexture(), builder::metallicRoughnessMap, false);
            loadTextureRef(data.normalTexture(), builder::normalMap, false);
            loadTextureRef(data.occlusionTexture(), builder::occlusionMap, false);
            loadTextureRef(data.emissiveTexture(), builder::emissiveMap, true);
        }

        return builder.build();
    }

    private void loadTextureRef(org.chapzlock.core.asset.gltf.GltfTextureRef ref,
                                java.util.function.Consumer<Texture> setter, boolean srgb) {
        if (ref == null) return;
        Texture tex = ref.isEmbedded()
            ? textureRegistry.loadFromMemory(ref.imageBytes(), ref.cacheKey(), srgb, false)
            : textureRegistry.load(ref.filePath(), srgb, false);
        setter.accept(tex);
    }

    /**
     * Creates Skeleton components for all nodes that reference a skin.
     *
     * <p>A skin in glTF defines which joints (nodes) influence a mesh and provides the
     * inverse bind matrices needed for GPU skinning. This method remaps the joint node
     * indices to entity IDs (since entities were created in Phase 1) and attaches a
     * Skeleton component to the skinned mesh entity.
     *
     * @param asset     the glTF asset with skin data
     * @param nodes     the flat list of glTF nodes
     * @param entityIds mapping from node index to entity ID
     */
    private void instantiateSkeletons(GltfAsset asset, List<GltfNodeData> nodes, int[] entityIds) {
        for (int i = 0; i < nodes.size(); i++) {
            GltfNodeData node = nodes.get(i);
            if (node.skinIndex() < 0) continue;
            if (node.skinIndex() >= asset.skins().size()) continue;

            GltfSkinData skinData = asset.skins().get(node.skinIndex());
            int[] jointNodeIndices = skinData.jointNodeIndices();
            int[] jointEntities = new int[jointNodeIndices.length];

            for (int j = 0; j < jointNodeIndices.length; j++) {
                jointEntities[j] = entityIds[jointNodeIndices[j]];
            }

            Skeleton skeleton = new Skeleton(jointEntities, skinData.inverseBindMatrices());
            registry.addComponent(entityIds[i], skeleton);
        }
    }

    /**
     * Converts glTF animation data into engine Animation objects and attaches an
     * AnimationClipSet to the root entity.
     *
     * <p>Each glTF animation becomes an engine {@link Animation} with channels remapped
     * from node indices to entity IDs. All clips are collected into an {@link AnimationClipSet}
     * on the root entity so game code can switch between them at runtime.
     *
     * @param asset        the glTF asset containing animation data
     * @param entityIds    mapping from node index to entity ID
     * @param rootEntityId the root entity to attach the clip set to
     */
    private void instantiateAnimations(GltfAsset asset, int[] entityIds, int rootEntityId) {
        if (asset.animations().isEmpty()) return;

        Map<String, Animation> clips = new HashMap<>();

        for (GltfAnimationData animData : asset.animations()) {
            List<Channel> channels = new ArrayList<>();

            for (GltfAnimationData.ChannelData channelData : animData.channels()) {
                int targetEntity = entityIds[channelData.targetNodeIndex()];
                TargetPath path = switch (channelData.path()) {
                    case "translation" -> TargetPath.TRANSLATION;
                    case "rotation" -> TargetPath.ROTATION;
                    case "scale" -> TargetPath.SCALE;
                    case "weights" -> TargetPath.WEIGHTS;
                    default -> null;
                };
                if (path == null) continue;

                Interpolation interpolation = switch (channelData.interpolation()) {
                    case "STEP" -> Interpolation.STEP;
                    case "CUBICSPLINE" -> Interpolation.CUBICSPLINE;
                    default -> Interpolation.LINEAR;
                };

                channels.add(new Channel(targetEntity, path, interpolation,
                    channelData.keyframeTimes(), channelData.keyframeValues()));
            }

            String name = animData.name() != null ? animData.name() : "animation_" + clips.size();
            clips.put(name, new Animation(name, animData.duration(), channels));
        }

        registry.addComponent(rootEntityId, new AnimationClipSet(clips));

        AnimationState state = new AnimationState(null);
        state.setPlaying(false);
        registry.addComponent(rootEntityId, state);
    }
}
