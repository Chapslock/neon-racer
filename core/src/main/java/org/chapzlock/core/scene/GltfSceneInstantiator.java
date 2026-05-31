package org.chapzlock.core.scene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.gltf.GltfAsset;
import org.chapzlock.core.asset.gltf.GltfMaterialData;
import org.chapzlock.core.asset.gltf.GltfMeshData;
import org.chapzlock.core.asset.gltf.GltfNodeData;
import org.chapzlock.core.component.AlphaMode;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.OpaqueLayer;
import org.chapzlock.core.component.PbrMaterial;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.component.WorldTransform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.PbrShaderProps;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureSystem;

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
 *   <li>For each glTF node: create an entity with Transform, Hierarchy, WorldTransform</li>
 *   <li>If the node has a mesh: upload to GPU via MeshSystem, add Mesh + PbrMaterial + OpaqueLayer</li>
 *   <li>Wire up parent-child relationships in Hierarchy components</li>
 *   <li>Track named nodes for later lookup via GltfInstance</li>
 * </ol>
 *
 * <p><strong>When to use:</strong> Called by SceneManager when loading a glTF scene
 * or instantiating a prefab. Game code doesn't call this directly.
 *
 * <p><strong>Three-phase approach:</strong> The instantiation process is split into three
 * phases to ensure all entities exist before hierarchy links are created, and all hierarchy
 * is resolved before meshes reference parent entities:
 * <ol>
 *   <li><em>Phase 1:</em> Create all entities and set their local transforms (position,
 *       rotation, scale from the glTF node data).</li>
 *   <li><em>Phase 2:</em> Wire up parent-child hierarchy relationships now that all entity
 *       IDs are known.</li>
 *   <li><em>Phase 3:</em> Upload mesh geometry to the GPU and create PBR material components
 *       for nodes that reference meshes.</li>
 * </ol>
 *
 * @see GltfAsset
 * @see GltfInstance
 */
public class GltfSceneInstantiator {

    private final ComponentRegistry registry;
    private final MeshSystem meshSystem;
    private final TextureSystem textureSystem;
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
    public GltfSceneInstantiator(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.meshSystem = ctx.getMeshSystem();
        this.textureSystem = ctx.getTextureSystem();
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
     *   <li>A {@link WorldTransform} is added (computed later by TransformSystem)</li>
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
            transform.setScale(node.scale().x); // uniform scale (use X component)

            // WorldTransform is an empty placeholder computed by TransformSystem each frame
            WorldTransform worldTransform = new WorldTransform();
            registry.addComponents(entityId, transform, worldTransform);

            // Track named nodes so gameplay code can find them via GltfInstance.entity("name")
            if (node.name() != null && !node.name().isEmpty()) {
                nameMap.put(node.name(), entityId);
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
        if (meshSystem != null && shaderSystem != null) {
            Shader pbrShader = getOrCreatePbrShader();
            instantiateMeshes(asset, nodes, entityIds, pbrShader);
        }

        // Determine root entity ID: use the first root node from the asset,
        // or fall back to the first entity if no roots are specified.
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
                Mesh mesh = meshSystem.load(primitive.meshData());

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
                    WorldTransform subWt = new WorldTransform();
                    Hierarchy subHierarchy = Hierarchy.childOf(entityIds[i]);
                    registry.addComponents(subEntity, subTransform, subWt, subHierarchy, mesh, pbrMaterial, new OpaqueLayer());

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
     * PbrMaterial builder. Textures are loaded from their file paths via TextureSystem.
     * If a texture path is null, that texture slot is left empty and the shader will use
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

        // Parse alpha mode string (glTF uses strings) to our enum
        builder.alphaMode(switch (data.alphaMode()) {
            case "MASK" -> AlphaMode.MASK;
            case "BLEND" -> AlphaMode.BLEND;
            default -> AlphaMode.OPAQUE;
        });

        // Load textures from file paths if specified.
        // Each texture is uploaded to the GPU and cached by TextureSystem.
        if (textureSystem != null) {
            if (data.baseColorTexturePath() != null) {
                builder.baseColorMap(textureSystem.load(data.baseColorTexturePath()));
            }
            if (data.metallicRoughnessTexturePath() != null) {
                builder.metallicRoughnessMap(textureSystem.load(data.metallicRoughnessTexturePath()));
            }
            if (data.normalTexturePath() != null) {
                builder.normalMap(textureSystem.load(data.normalTexturePath()));
            }
            if (data.occlusionTexturePath() != null) {
                builder.occlusionMap(textureSystem.load(data.occlusionTexturePath()));
            }
            if (data.emissiveTexturePath() != null) {
                builder.emissiveMap(textureSystem.load(data.emissiveTexturePath()));
            }
        }

        return builder.build();
    }
}
