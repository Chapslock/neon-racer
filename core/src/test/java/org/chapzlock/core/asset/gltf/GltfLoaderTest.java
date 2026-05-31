package org.chapzlock.core.asset.gltf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.*;

class GltfLoaderTest {

    private boolean testGlbExists() {
        return getClass().getClassLoader().getResource("models/test-cube.glb") != null;
    }

    @Test
    @EnabledIf("testGlbExists")
    void loadTestCubeGlb() {
        GltfLoader loader = new GltfLoader();
        GltfAsset asset = loader.load("models/test-cube.glb");

        assertNotNull(asset);
        assertFalse(asset.nodes().isEmpty(), "Should have at least one node");
        assertFalse(asset.meshes().isEmpty(), "Should have at least one mesh");
        assertFalse(asset.rootNodeIndices().isEmpty(), "Should have root nodes");

        // Verify mesh has positions
        GltfMeshData mesh = asset.meshes().get(0);
        assertFalse(mesh.primitives().isEmpty(), "Mesh should have primitives");
        assertTrue(mesh.primitives().get(0).meshData().getPositions().length > 0,
            "Primitive should have positions");
    }

    @Test
    void loadNonexistentFileThrowsGltfLoadException() {
        GltfLoader loader = new GltfLoader();
        assertThrows(GltfLoadException.class, () -> loader.load("nonexistent.glb"));
    }
}
