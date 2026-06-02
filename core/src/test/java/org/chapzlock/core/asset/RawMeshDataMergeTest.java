package org.chapzlock.core.asset;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RawMeshDataMergeTest {

    @Test
    void merge_singleMesh_returnsSameMesh() {
        float[] positions = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        float[] texCoords = {0, 0, 1, 0, 0, 1};
        int[] indices = {0, 1, 2};

        RawMeshData mesh = new RawMeshData(positions, texCoords, indices);
        RawMeshData result = RawMeshData.merge(List.of(mesh));

        assertSame(mesh, result);
    }

    @Test
    void merge_twoMeshes_concatenatesPositionsAndAdjustsIndices() {
        // First mesh: triangle at origin (3 vertices)
        float[] positions1 = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        float[] texCoords1 = {0, 0, 1, 0, 0, 1};
        int[] indices1 = {0, 1, 2};

        // Second mesh: triangle offset (3 vertices)
        float[] positions2 = {2, 0, 0, 3, 0, 0, 2, 1, 0};
        float[] texCoords2 = {0, 0, 1, 0, 0, 1};
        int[] indices2 = {0, 1, 2};

        RawMeshData mesh1 = new RawMeshData(positions1, texCoords1, indices1);
        RawMeshData mesh2 = new RawMeshData(positions2, texCoords2, indices2);

        RawMeshData result = RawMeshData.merge(List.of(mesh1, mesh2));

        // Positions concatenated
        float[] expectedPositions = {0, 0, 0, 1, 0, 0, 0, 1, 0, 2, 0, 0, 3, 0, 0, 2, 1, 0};
        assertArrayEquals(expectedPositions, result.getPositions());

        // TexCoords concatenated
        float[] expectedTexCoords = {0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1};
        assertArrayEquals(expectedTexCoords, result.getTexCoords());

        // Indices: second mesh offset by 3 (vertex count of first mesh)
        int[] expectedIndices = {0, 1, 2, 3, 4, 5};
        assertArrayEquals(expectedIndices, result.getIndices());
    }

    @Test
    void merge_preservesNormalsWhenPresent() {
        float[] positions1 = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        float[] texCoords1 = {0, 0, 1, 0, 0, 1};
        int[] indices1 = {0, 1, 2};
        float[] normals1 = {0, 0, 1, 0, 0, 1, 0, 0, 1};

        float[] positions2 = {2, 0, 0, 3, 0, 0, 2, 1, 0};
        float[] texCoords2 = {0, 0, 1, 0, 0, 1};
        int[] indices2 = {0, 1, 2};
        float[] normals2 = {0, 1, 0, 0, 1, 0, 0, 1, 0};

        RawMeshData mesh1 = new RawMeshData(positions1, texCoords1, indices1, normals1);
        RawMeshData mesh2 = new RawMeshData(positions2, texCoords2, indices2, normals2);

        RawMeshData result = RawMeshData.merge(List.of(mesh1, mesh2));

        assertTrue(result.hasNormals());
        float[] expectedNormals = {0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0};
        assertArrayEquals(expectedNormals, result.getNormals());
    }

    @Test
    void merge_emptyList_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> RawMeshData.merge(List.of()));
    }

    @Test
    void merge_dropsNormalsWhenNotAllMeshesHaveThem() {
        float[] positions1 = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        float[] texCoords1 = {0, 0, 1, 0, 0, 1};
        int[] indices1 = {0, 1, 2};
        float[] normals1 = {0, 0, 1, 0, 0, 1, 0, 0, 1};

        float[] positions2 = {2, 0, 0, 3, 0, 0, 2, 1, 0};
        float[] texCoords2 = {0, 0, 1, 0, 0, 1};
        int[] indices2 = {0, 1, 2};

        RawMeshData mesh1 = new RawMeshData(positions1, texCoords1, indices1, normals1);
        RawMeshData mesh2 = new RawMeshData(positions2, texCoords2, indices2); // no normals

        RawMeshData result = RawMeshData.merge(List.of(mesh1, mesh2));

        assertFalse(result.hasNormals());
    }

    @Test
    void merge_threeMeshes_offsetsIndicesCorrectly() {
        // 2 vertices each
        RawMeshData mesh1 = RawMeshData.builder()
            .positions(new float[]{0, 0, 0, 1, 0, 0})
            .texCoords(new float[]{0, 0, 1, 0})
            .indices(new int[]{0, 1, 0})
            .build();

        RawMeshData mesh2 = RawMeshData.builder()
            .positions(new float[]{2, 0, 0, 3, 0, 0, 4, 0, 0})
            .texCoords(new float[]{0, 0, 1, 0, 0.5f, 0.5f})
            .indices(new int[]{0, 1, 2})
            .build();

        RawMeshData mesh3 = RawMeshData.builder()
            .positions(new float[]{5, 0, 0, 6, 0, 0})
            .texCoords(new float[]{0, 0, 1, 0})
            .indices(new int[]{0, 1, 0})
            .build();

        RawMeshData result = RawMeshData.merge(List.of(mesh1, mesh2, mesh3));

        // mesh1: 2 verts, offset 0 -> indices {0, 1, 0}
        // mesh2: 3 verts, offset 2 -> indices {2, 3, 4}
        // mesh3: 2 verts, offset 5 -> indices {5, 6, 5}
        int[] expectedIndices = {0, 1, 0, 2, 3, 4, 5, 6, 5};
        assertArrayEquals(expectedIndices, result.getIndices());
    }
}
