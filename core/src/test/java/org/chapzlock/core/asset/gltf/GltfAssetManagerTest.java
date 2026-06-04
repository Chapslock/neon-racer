package org.chapzlock.core.asset.gltf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GltfAssetManagerTest {

    @Test
    void getOrLoadReturnsSameInstanceOnSecondCall() {
        GltfAssetManager manager = new GltfAssetManager(null, null, null, null);
        GltfAsset first = manager.getOrLoad("models/test-cube.glb");
        GltfAsset second = manager.getOrLoad("models/test-cube.glb");
        assertSame(first, second);
    }

    @Test
    void clearRemovesCachedAssets() {
        GltfAssetManager manager = new GltfAssetManager(null, null, null, null);
        GltfAsset first = manager.getOrLoad("models/test-cube.glb");
        manager.clear();
        GltfAsset afterClear = manager.getOrLoad("models/test-cube.glb");
        assertNotSame(first, afterClear);
    }
}
