package org.chapzlock.core.asset.gltf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.animation.Channel;
import org.chapzlock.core.animation.Interpolation;
import org.chapzlock.core.animation.TargetPath;
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
import org.chapzlock.core.render.ShaderRegistry;
import org.chapzlock.core.render.TextureRegistry;

/**
 * Static utility methods implementing the five-phase glTF entity expansion pipeline.
 *
 * <p>This class extracts the core logic for converting a parsed {@link GltfAsset} into
 * ECS entities with the appropriate components. Each phase is a standalone static method
 * that operates on the entity ID array produced by Phase 1.
 *
 * <p><strong>Five-phase pipeline:</strong>
 * <ol>
 *   <li>{@link #createNodes} — create entities with local transforms from glTF nodes</li>
 *   <li>{@link #wireHierarchy} — wire parent-child Hierarchy components</li>
 *   <li>{@link #uploadMeshesAndMaterials} — upload GPU resources, attach Mesh + PbrMaterial</li>
 *   <li>{@link #createSkeletons} — create Skeleton components for skinned meshes</li>
 *   <li>{@link #createAnimations} — create AnimationClipSet + AnimationState on parent</li>
 * </ol>
 *
 * <p>In this utility, all root nodes become children of a provided parent entity. There is
 * no standalone "root" mode — callers always provide an anchor entity that owns the model.
 *
 * @see GltfAsset
 */
class GltfUtil {

    private GltfUtil() {
        // Static utility — not instantiable
    }

    /**
     * Phase 1: Creates entities with local transforms from glTF nodes.
     *
     * <p>For each node in the asset, an entity is created and given a {@link Transform}
     * component initialized from the node's TRS (Translation, Rotation, Scale). Named
     * nodes also receive a {@link Name} component for runtime lookup.
     *
     * <p>All entities are created upfront so that subsequent phases can reference any
     * entity by its node index (a child node may appear before its parent in the list).
     *
     * @param asset         the parsed glTF data containing nodes
     * @param parentEntity  the entity that will own this model (used for reference only in this phase)
     * @param registry      the component registry for entity creation and component attachment
     * @return an array of entity IDs, indexed by glTF node index
     */
    static int[] createNodes(GltfAsset asset, int parentEntity, ComponentRegistry registry) {
        List<GltfNodeData> nodes = asset.nodes();
        int[] entityIds = new int[nodes.size()];

        for (int i = 0; i < nodes.size(); i++) {
            GltfNodeData node = nodes.get(i);
            int entityId = Entity.create(registry);
            entityIds[i] = entityId;

            Transform transform = new Transform();
            transform.setPosition(node.translation());
            transform.setRotationQuaternion(node.rotation());
            transform.setScale(node.scale().x, node.scale().y, node.scale().z);

            registry.addComponent(entityId, transform);

            if (node.name() != null && !node.name().isEmpty()) {
                registry.addComponent(entityId, new Name(node.name()));
            }
        }

        return entityIds;
    }

    /**
     * Phase 2: Wires Hierarchy components linking nodes into a parent-child tree.
     *
     * <p>Root nodes (those listed in {@link GltfAsset#rootNodeIndices()}) become children
     * of the provided parent entity via {@link Hierarchy#childOf(int)}. Non-root nodes
     * get their parent from the glTF tree structure.
     *
     * <p>Additionally, root entities are registered as children in the parent entity's
     * Hierarchy component. If the parent has no Hierarchy, one is created as a root.
     *
     * @param asset        the parsed glTF data with hierarchy information
     * @param entityIds    the entity ID array from Phase 1
     * @param parentEntity the entity to parent root nodes under
     * @param registry     the component registry
     */
    static void wireHierarchy(GltfAsset asset, int[] entityIds, int parentEntity, ComponentRegistry registry) {
        List<GltfNodeData> nodes = asset.nodes();

        for (int i = 0; i < nodes.size(); i++) {
            GltfNodeData node = nodes.get(i);
            List<Integer> childIndices = node.childIndices();

            boolean isRoot = asset.rootNodeIndices().contains(i);
            Hierarchy hierarchy;
            if (isRoot) {
                hierarchy = Hierarchy.childOf(parentEntity);
            } else {
                hierarchy = findParentHierarchy(i, nodes, entityIds);
            }

            for (int childIdx : childIndices) {
                hierarchy.addChild(entityIds[childIdx]);
            }

            registry.addComponent(entityIds[i], hierarchy);
        }

        // Register root entities as children in the parent's Hierarchy component.
        Hierarchy parentHierarchy = registry.getComponent(parentEntity, Hierarchy.class);
        if (parentHierarchy == null) {
            parentHierarchy = Hierarchy.root();
            registry.addComponent(parentEntity, parentHierarchy);
        }
        for (int rootIdx : asset.rootNodeIndices()) {
            parentHierarchy.addChild(entityIds[rootIdx]);
        }
    }

    /**
     * Phase 3: Uploads mesh geometry and creates PBR materials for nodes with meshes.
     *
     * <p>For each node referencing a mesh (meshIndex >= 0), this method uploads the vertex
     * data to the GPU and creates a PBR material from the glTF material definition. The
     * first primitive of a mesh is attached to the node's own entity; additional primitives
     * get their own child entities (since each entity can only hold one Mesh component).
     *
     * @param asset            the parsed glTF data with mesh and material information
     * @param entityIds        the entity ID array from Phase 1
     * @param meshRegistry     the GPU mesh upload registry
     * @param textureRegistry  the GPU texture loading registry
     * @param shaderRegistry   the shader compilation registry
     * @param registry         the component registry
     */
    static void uploadMeshesAndMaterials(GltfAsset asset, int[] entityIds,
                                         MeshRegistry meshRegistry, TextureRegistry textureRegistry,
                                         ShaderRegistry shaderRegistry, ComponentRegistry registry) {
        List<GltfNodeData> nodes = asset.nodes();
        Shader pbrShader = getOrCreatePbrShader(shaderRegistry);

        for (int i = 0; i < nodes.size(); i++) {
            GltfNodeData node = nodes.get(i);
            if (node.meshIndex() < 0) continue;

            GltfMeshData meshData = asset.meshes().get(node.meshIndex());
            List<GltfMeshData.Primitive> primitives = meshData.primitives();

            for (int primIdx = 0; primIdx < primitives.size(); primIdx++) {
                GltfMeshData.Primitive primitive = primitives.get(primIdx);

                Mesh mesh = meshRegistry.load(primitive.meshData());

                GltfMaterialData matData = primitive.materialIndex() >= 0
                    ? asset.materials().get(primitive.materialIndex())
                    : GltfMaterialData.defaultMaterial();

                PbrMaterial pbrMaterial = buildPbrMaterial(pbrShader, matData, textureRegistry);

                if (primIdx == 0) {
                    registry.addComponents(entityIds[i], mesh, pbrMaterial, new OpaqueLayer());
                } else {
                    int subEntity = Entity.create(registry);
                    Transform subTransform = new Transform();
                    Hierarchy subHierarchy = Hierarchy.childOf(entityIds[i]);
                    registry.addComponents(subEntity, subTransform, subHierarchy, mesh, pbrMaterial, new OpaqueLayer());

                    Hierarchy parentHierarchy = registry.getComponent(entityIds[i], Hierarchy.class);
                    if (parentHierarchy != null) {
                        parentHierarchy.addChild(subEntity);
                    }
                }
            }
        }
    }

    /**
     * Phase 4: Creates Skeleton components for skinned mesh nodes.
     *
     * <p>A skin in glTF defines which joints (nodes) influence a mesh and provides the
     * inverse bind matrices needed for GPU skinning. This method remaps joint node indices
     * to entity IDs and attaches a Skeleton component to each skinned mesh entity.
     *
     * @param asset     the parsed glTF data with skin definitions
     * @param entityIds the entity ID array from Phase 1
     * @param registry  the component registry
     */
    static void createSkeletons(GltfAsset asset, int[] entityIds, ComponentRegistry registry) {
        List<GltfNodeData> nodes = asset.nodes();

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
     * Phase 5: Creates AnimationClipSet and AnimationState on the parent entity.
     *
     * <p>Each glTF animation becomes an engine {@link Animation} with channels remapped
     * from node indices to entity IDs. All clips are collected into an {@link AnimationClipSet}
     * on the parent entity so game code can control animations from the owning entity.
     *
     * <p>If the asset has no animations, this method does nothing.
     *
     * @param asset        the parsed glTF data containing animation clips
     * @param entityIds    the entity ID array from Phase 1
     * @param parentEntity the entity to attach AnimationClipSet and AnimationState to
     * @param registry     the component registry
     */
    static void createAnimations(GltfAsset asset, int[] entityIds, int parentEntity, ComponentRegistry registry) {
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

        registry.addComponent(parentEntity, new AnimationClipSet(clips));

        AnimationState state = new AnimationState(null);
        state.setPlaying(false);
        registry.addComponent(parentEntity, state);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Finds the parent of a node by scanning all nodes' child indices.
     *
     * <p>Performs a linear scan through all nodes looking for one whose {@code childIndices}
     * contains the target index. Returns a {@link Hierarchy#childOf(int)} with the parent's
     * entity ID, or {@link Hierarchy#root()} if no parent is found (defensive handling for
     * malformed data).
     *
     * @param nodeIndex the index of the node to find the parent for
     * @param nodes     the complete list of nodes in the asset
     * @param entityIds mapping from node index to entity ID
     * @return a Hierarchy configured with the correct parent
     */
    private static Hierarchy findParentHierarchy(int nodeIndex, List<GltfNodeData> nodes, int[] entityIds) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).childIndices().contains(nodeIndex)) {
                return Hierarchy.childOf(entityIds[i]);
            }
        }
        // No parent found — defensive fallback for malformed glTF data
        return Hierarchy.root();
    }

    /**
     * Converts glTF material data into a PbrMaterial component.
     *
     * <p>Maps all glTF PBR properties (factors, textures, alpha mode) to the engine's
     * PbrMaterial builder. Textures are loaded via TextureRegistry when available.
     *
     * @param shader          the compiled PBR shader
     * @param data            the parsed material properties from the glTF file
     * @param textureRegistry the texture loading registry (may be null in test mode)
     * @return a fully configured PbrMaterial component
     */
    private static PbrMaterial buildPbrMaterial(Shader shader, GltfMaterialData data, TextureRegistry textureRegistry) {
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
            loadTextureRef(data.baseColorTexture(), builder::baseColorMap, true, textureRegistry);
            loadTextureRef(data.metallicRoughnessTexture(), builder::metallicRoughnessMap, false, textureRegistry);
            loadTextureRef(data.normalTexture(), builder::normalMap, false, textureRegistry);
            loadTextureRef(data.occlusionTexture(), builder::occlusionMap, false, textureRegistry);
            loadTextureRef(data.emissiveTexture(), builder::emissiveMap, true, textureRegistry);
        }

        return builder.build();
    }

    /**
     * Loads a texture from a glTF texture reference and passes it to a setter.
     *
     * <p>Dispatches on whether the texture is file-based (external URI) or memory-based
     * (embedded in GLB). If the reference is null, this method does nothing.
     *
     * @param ref             the texture reference (nullable)
     * @param setter          the consumer to receive the loaded texture
     * @param srgb            whether to load in sRGB color space
     * @param textureRegistry the texture loading registry
     */
    private static void loadTextureRef(GltfTextureRef ref, Consumer<Texture> setter, boolean srgb, TextureRegistry textureRegistry) {
        if (ref == null) return;
        Texture tex = ref.isEmbedded()
            ? textureRegistry.loadFromMemory(ref.imageBytes(), ref.cacheKey(), srgb, false)
            : textureRegistry.load(ref.filePath(), srgb, false);
        setter.accept(tex);
    }

    /**
     * Gets or creates the shared PBR shader instance.
     */
    private static Shader getOrCreatePbrShader(ShaderRegistry shaderRegistry) {
        Shader shader = new Shader(PbrShaderProps.VERTEX_FILE, PbrShaderProps.FRAGMENT_FILE);
        shaderRegistry.compileAndLink(shader);
        return shader;
    }
}
