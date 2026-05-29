package org.chapzlock.core.scene;

import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefCountTest {

    @Test
    void meshSystemRetainIncreasesCount() {
        MeshSystem meshSystem = new MeshSystem();
        meshSystem.retain(1);
        meshSystem.retain(1);
        assertEquals(2, meshSystem.getRefCount(1));
    }

    @Test
    void meshSystemReleaseDecreasesCount() {
        MeshSystem meshSystem = new MeshSystem();
        meshSystem.retain(1);
        meshSystem.retain(1);
        meshSystem.release(1);
        assertEquals(1, meshSystem.getRefCount(1));
    }

    @Test
    void meshSystemReleaseToZeroRemovesTracking() {
        MeshSystem meshSystem = new MeshSystem();
        meshSystem.retain(1);
        meshSystem.release(1);
        assertEquals(0, meshSystem.getRefCount(1));
    }

    @Test
    void textureSystemRetainAndRelease() {
        TextureSystem textureSystem = new TextureSystem();
        textureSystem.retain(5);
        textureSystem.retain(5);
        assertEquals(2, textureSystem.getRefCount(5));
        textureSystem.release(5);
        assertEquals(1, textureSystem.getRefCount(5));
    }

    @Test
    void shaderSystemRetainAndRelease() {
        ShaderSystem shaderSystem = new ShaderSystem();
        shaderSystem.retain(10);
        assertEquals(1, shaderSystem.getRefCount(10));
        shaderSystem.release(10);
        assertEquals(0, shaderSystem.getRefCount(10));
    }
}
