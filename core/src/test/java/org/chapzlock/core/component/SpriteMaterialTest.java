package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpriteMaterialTest {

    @Test
    void pathConstructor_storesPathAndDefaultTint() {
        SpriteMaterial mat = new SpriteMaterial("textures/hud.png");
        assertEquals("textures/hud.png", mat.texturePath());
        assertNull(mat.texture());
        assertEquals(new Vector4f(1f, 1f, 1f, 1f), mat.tint());
    }

    @Test
    void pathWithTintConstructor_storesPathAndTint() {
        Vector4f tint = new Vector4f(1f, 0f, 0f, 0.5f);
        SpriteMaterial mat = new SpriteMaterial("textures/red.png", tint);
        assertEquals("textures/red.png", mat.texturePath());
        assertNull(mat.texture());
        assertEquals(tint, mat.tint());
    }

    @Test
    void textureConstructor_storesTextureAndNoPath() {
        Texture tex = Texture.builder().id(42).build();
        SpriteMaterial mat = new SpriteMaterial(tex);
        assertSame(tex, mat.texture());
        assertNull(mat.texturePath());
        assertEquals(new Vector4f(1f, 1f, 1f, 1f), mat.tint());
    }

    @Test
    void textureWithTintConstructor_storesBoth() {
        Texture tex = Texture.builder().id(7).build();
        Vector4f tint = new Vector4f(0f, 1f, 0f, 1f);
        SpriteMaterial mat = new SpriteMaterial(tex, tint);
        assertSame(tex, mat.texture());
        assertEquals(tint, mat.tint());
        assertNull(mat.texturePath());
    }

    @Test
    void needsResolution_trueWhenPathSetAndTextureNull() {
        SpriteMaterial mat = new SpriteMaterial("textures/hud.png");
        assertTrue(mat.needsResolution());
    }

    @Test
    void needsResolution_falseWhenTextureLoaded() {
        SpriteMaterial mat = new SpriteMaterial(Texture.builder().id(1).build());
        assertFalse(mat.needsResolution());
    }

    @Test
    void withTexture_returnsResolvedCopy() {
        SpriteMaterial unresolved = new SpriteMaterial("textures/hud.png");
        Texture loadedTex = Texture.builder().id(99).build();
        SpriteMaterial resolved = unresolved.withTexture(loadedTex);
        assertSame(loadedTex, resolved.texture());
        assertEquals("textures/hud.png", resolved.texturePath());
        assertFalse(resolved.needsResolution());
    }

    @Test
    void implementsComponent() {
        assertInstanceOf(Component.class, new SpriteMaterial("test.png"));
    }
}
