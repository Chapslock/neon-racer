package org.chapzlock.core.asset.gltf;

import java.util.HashMap;
import java.util.Map;

import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityFactory;
import org.chapzlock.core.render.MeshRegistry;
import org.chapzlock.core.render.ShaderRegistry;
import org.chapzlock.core.render.TextureRegistry;

/**
 * Manages loading and instantiation of glTF assets.
 *
 * <p><strong>What this does:</strong> Combines glTF asset caching (previously in GltfAssetRegistry)
 * with instantiation orchestration. The {@link #instantiate(String, int)} method drives the
 * five-phase entity expansion pipeline defined in {@link GltfUtil}, converting a parsed
 * {@link GltfAsset} into live ECS entities with GPU resources.
 *
 * <p><strong>Caching:</strong> Parsed glTF files are cached by resource path. If the same model
 * is instantiated multiple times (e.g., spawning multiple copies of a car), the file is only
 * parsed once. GPU resources (meshes, textures) are managed separately by their respective
 * registries with reference counting.
 *
 * <p><strong>Lifecycle:</strong> Owned by {@link org.chapzlock.core.application.Engine}.
 * Call {@link #clear()} to free cached parse data if assets are no longer needed.
 *
 * @see GltfAsset
 * @see GltfUtil
 * @see GltfLoader
 */
public class GltfAssetManager {

    private final Map<String, GltfAsset> cache = new HashMap<>();
    private final GltfLoader loader = new GltfLoader();
    private final ComponentRegistry registry;
    private final MeshRegistry meshRegistry;
    private final TextureRegistry textureRegistry;
    private final ShaderRegistry shaderRegistry;
    private EntityFactory entityFactory;

    /**
     * Creates a new glTF asset manager with access to GPU registries for instantiation.
     *
     * <p>In test contexts, GPU registries may be null — instantiation will skip mesh/material
     * upload in that case (Phase 3 is skipped).
     *
     * @param registry        the component registry for entity creation
     * @param meshRegistry    the GPU mesh registry (nullable for tests)
     * @param textureRegistry the GPU texture registry (nullable for tests)
     * @param shaderRegistry  the GPU shader registry (nullable for tests)
     */
    public GltfAssetManager(ComponentRegistry registry,
                            MeshRegistry meshRegistry,
                            TextureRegistry textureRegistry,
                            ShaderRegistry shaderRegistry) {
        this.registry = registry;
        this.meshRegistry = meshRegistry;
        this.textureRegistry = textureRegistry;
        this.shaderRegistry = shaderRegistry;
    }

    /**
     * Sets the entity factory for hierarchy wiring operations.
     * Called after construction to break the circular dependency between
     * EntityFactory and GltfAssetManager.
     *
     * @param entityFactory the entity factory
     */
    public void setEntityFactory(EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    /**
     * Returns a cached asset or loads it from disk if not yet cached.
     *
     * @param resourcePath path relative to resources root (e.g., "glb/car.glb")
     * @return the parsed asset (never null; throws on load failure)
     * @throws GltfLoadException if the file cannot be loaded or parsed
     */
    public GltfAsset getOrLoad(String resourcePath) {
        return cache.computeIfAbsent(resourcePath, loader::load);
    }

    /**
     * Loads (if needed) and instantiates a glTF asset as child entities of the given parent.
     *
     * <p>This orchestrates the five-phase pipeline defined in {@link GltfUtil}:
     * <ol>
     *   <li>Create node entities with transforms</li>
     *   <li>Wire parent-child hierarchy</li>
     *   <li>Upload meshes and materials to GPU (skipped if GPU registries are null)</li>
     *   <li>Create skeleton components for skinned meshes</li>
     *   <li>Create animation clips on the parent entity</li>
     * </ol>
     *
     * @param resourcePath path relative to resources root (e.g., "glb/car.glb")
     * @param parentEntity the entity that will own this model instance
     * @throws GltfLoadException if the file cannot be loaded or parsed
     */
    public void instantiate(String resourcePath, int parentEntity) {
        GltfAsset asset = getOrLoad(resourcePath);
        instantiate(asset, parentEntity);
    }

    /**
     * Instantiates a pre-loaded glTF asset as child entities of the given parent.
     *
     * <p>Package-private overload for testing with in-memory assets (no file on disk).
     *
     * @param asset        the parsed glTF asset to instantiate
     * @param parentEntity the entity that will own this model instance
     */
    void instantiate(GltfAsset asset, int parentEntity) {
        int[] entityIds = GltfUtil.createNodes(asset, parentEntity, registry);
        GltfUtil.wireHierarchy(asset, entityIds, parentEntity, entityFactory);
        if (meshRegistry != null && shaderRegistry != null) {
            GltfUtil.uploadMeshesAndMaterials(asset, entityIds, meshRegistry, textureRegistry, shaderRegistry, registry, entityFactory);
        }
        GltfUtil.createSkeletons(asset, entityIds, registry);
        GltfUtil.createAnimations(asset, entityIds, parentEntity, registry);
    }

    /**
     * Removes all cached assets, freeing memory.
     * Does NOT affect GPU resources — those are managed by MeshRegistry/TextureRegistry.
     */
    public void clear() {
        cache.clear();
    }
}
