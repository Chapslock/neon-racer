package org.chapzlock.core.asset.gltf;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

class GltfTextureRefTest {

    @Test
    void fromPathCreatesNonEmbeddedRef() {
        GltfTextureRef ref = GltfTextureRef.fromPath("textures/albedo.png");
        assertFalse(ref.isEmbedded());
        assertEquals("textures/albedo.png", ref.filePath());
        assertEquals("textures/albedo.png", ref.cacheKey());
        assertNull(ref.imageBytes());
    }

    @Test
    void fromMemoryCreatesEmbeddedRef() {
        ByteBuffer bytes = ByteBuffer.allocate(16);
        GltfTextureRef ref = GltfTextureRef.fromMemory(bytes, "embedded:model.glb:tex0");
        assertTrue(ref.isEmbedded());
        assertNull(ref.filePath());
        assertEquals("embedded:model.glb:tex0", ref.cacheKey());
        assertSame(bytes, ref.imageBytes());
    }
}
