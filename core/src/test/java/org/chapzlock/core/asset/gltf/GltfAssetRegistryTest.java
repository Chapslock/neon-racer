package org.chapzlock.core.asset.gltf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GltfAssetRegistryTest {

    @Test
    void getOrLoadReturnsSameInstanceOnSecondCall() {
        GltfAssetRegistry registry = new GltfAssetRegistry();
        GltfAsset first = registry.getOrLoad("models/test-cube.glb");
        GltfAsset second = registry.getOrLoad("models/test-cube.glb");
        assertSame(first, second);
    }

    @Test
    void clearRemovesCachedAssets() {
        GltfAssetRegistry registry = new GltfAssetRegistry();
        GltfAsset first = registry.getOrLoad("models/test-cube.glb");
        registry.clear();
        GltfAsset afterClear = registry.getOrLoad("models/test-cube.glb");
        assertNotSame(first, afterClear);
    }
}
