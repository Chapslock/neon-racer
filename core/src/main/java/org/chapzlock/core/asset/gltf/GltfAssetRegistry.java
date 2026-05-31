package org.chapzlock.core.asset.gltf;

import java.util.HashMap;
import java.util.Map;

/**
 * Caches parsed {@link GltfAsset} instances by resource path.
 *
 * <p><strong>Why cache?</strong> Parsing a GLB file involves reading binary buffers, decoding
 * accessors, and building node trees. If the same model is instantiated multiple times
 * (e.g., spawning multiple copies of a car), we only want to parse the file once and
 * re-instantiate from the cached CPU-side data.
 *
 * <p><strong>What is NOT cached here:</strong> GPU resources (meshes, textures) are managed
 * separately by MeshSystem and TextureSystem with their own reference counting. This registry
 * only caches the intermediate parse result ({@link GltfAsset}).
 *
 * <p><strong>Lifecycle:</strong> Owned by {@link org.chapzlock.core.application.EngineContext}.
 * Call {@link #clear()} to free memory if assets are no longer needed.
 *
 * @see GltfAsset
 * @see GltfLoader
 */
public class GltfAssetRegistry {

    private final Map<String, GltfAsset> cache = new HashMap<>();
    private final GltfLoader loader = new GltfLoader();

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
     * Removes all cached assets, freeing memory.
     * Does NOT affect GPU resources — those are managed by MeshSystem/TextureSystem.
     */
    public void clear() {
        cache.clear();
    }
}
