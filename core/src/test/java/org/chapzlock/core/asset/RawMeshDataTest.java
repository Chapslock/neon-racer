package org.chapzlock.core.asset;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RawMeshDataTest {

    @Test
    void basicConstructorSetsPositionsTexCoordsIndices() {
        float[] positions = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        float[] texCoords = {0, 0, 1, 0, 0, 1};
        int[] indices = {0, 1, 2};

        RawMeshData mesh = new RawMeshData(positions, texCoords, indices);

        assertArrayEquals(positions, mesh.getPositions());
        assertArrayEquals(texCoords, mesh.getTexCoords());
        assertArrayEquals(indices, mesh.getIndices());
        assertEquals(0, mesh.getNormals().length);
        assertFalse(mesh.hasTangents());
        assertFalse(mesh.hasJoints());
        assertFalse(mesh.hasWeights());
        assertFalse(mesh.hasColors());
        assertFalse(mesh.hasTexCoords1());
    }

    @Test
    void builderSetsAllAttributes() {
        float[] positions = {0, 0, 0};
        float[] texCoords = {0, 0};
        int[] indices = {0};
        float[] normals = {0, 1, 0};
        float[] tangents = {1, 0, 0, 1};
        int[] joints = {0, 1, 2, 3};
        float[] weights = {0.5f, 0.3f, 0.1f, 0.1f};
        float[] colors = {1, 0, 0, 1};
        float[] texCoords1 = {0.5f, 0.5f};

        RawMeshData mesh = RawMeshData.builder()
            .positions(positions)
            .texCoords(texCoords)
            .indices(indices)
            .normals(normals)
            .tangents(tangents)
            .joints(joints)
            .weights(weights)
            .colors(colors)
            .texCoords1(texCoords1)
            .build();

        assertArrayEquals(tangents, mesh.getTangents());
        assertArrayEquals(joints, mesh.getJoints());
        assertArrayEquals(weights, mesh.getWeights());
        assertArrayEquals(colors, mesh.getColors());
        assertArrayEquals(texCoords1, mesh.getTexCoords1());
        assertTrue(mesh.hasTangents());
        assertTrue(mesh.hasJoints());
        assertTrue(mesh.hasWeights());
        assertTrue(mesh.hasColors());
        assertTrue(mesh.hasTexCoords1());
    }

    @Test
    void legacyConstructorWithNormalsWorks() {
        float[] positions = {0, 0, 0};
        float[] texCoords = {0, 0};
        int[] indices = {0};
        float[] normals = {0, 1, 0};

        RawMeshData mesh = new RawMeshData(positions, texCoords, indices, normals);

        assertArrayEquals(normals, mesh.getNormals());
        assertTrue(mesh.hasNormals());
        assertFalse(mesh.hasTangents());
    }
}
