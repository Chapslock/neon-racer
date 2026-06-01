package org.chapzlock.core.component;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PbrMaterialTest {

    @Test
    void defaultsMatchGltfSpec() {
        PbrMaterial mat = PbrMaterial.builder().shader(null).build();

        assertEquals(new Vector4f(1, 1, 1, 1), mat.getBaseColorFactor());
        assertEquals(1.0f, mat.getMetallicFactor());
        assertEquals(1.0f, mat.getRoughnessFactor());
        assertEquals(new Vector3f(0, 0, 0), mat.getEmissiveFactor());
        assertEquals(AlphaMode.OPAQUE, mat.getAlphaMode());
        assertEquals(0.5f, mat.getAlphaCutoff());
        assertFalse(mat.isDoubleSided());
        assertNull(mat.getBaseColorMap());
        assertNull(mat.getNormalMap());
    }

    @Test
    void builderSetsAllFields() {
        PbrMaterial mat = PbrMaterial.builder()
            .shader(null)
            .baseColorFactor(new Vector4f(0.5f, 0, 0, 1))
            .metallicFactor(0.3f)
            .roughnessFactor(0.7f)
            .emissiveFactor(new Vector3f(1, 1, 0))
            .alphaMode(AlphaMode.MASK)
            .alphaCutoff(0.3f)
            .doubleSided(true)
            .build();

        assertEquals(0.3f, mat.getMetallicFactor());
        assertEquals(0.7f, mat.getRoughnessFactor());
        assertEquals(AlphaMode.MASK, mat.getAlphaMode());
        assertEquals(0.3f, mat.getAlphaCutoff());
        assertTrue(mat.isDoubleSided());
        assertEquals(new Vector4f(0.5f, 0, 0, 1), mat.getBaseColorFactor());
        assertEquals(new Vector3f(1, 1, 0), mat.getEmissiveFactor());
    }

    @Test
    void textureMapFieldsDefaultToNull() {
        PbrMaterial mat = PbrMaterial.builder().shader(null).build();

        assertNull(mat.getBaseColorMap());
        assertNull(mat.getMetallicRoughnessMap());
        assertNull(mat.getNormalMap());
        assertNull(mat.getOcclusionMap());
        assertNull(mat.getEmissiveMap());
    }

    @Test
    void implementsComponentInterface() {
        PbrMaterial mat = PbrMaterial.builder().shader(null).build();

        assertInstanceOf(org.chapzlock.core.ecs.Component.class, mat);
    }

    @Test
    void blendModeWithCustomCutoff() {
        PbrMaterial mat = PbrMaterial.builder()
            .shader(null)
            .alphaMode(AlphaMode.BLEND)
            .alphaCutoff(0.0f)
            .build();

        assertEquals(AlphaMode.BLEND, mat.getAlphaMode());
        assertEquals(0.0f, mat.getAlphaCutoff());
    }
}
