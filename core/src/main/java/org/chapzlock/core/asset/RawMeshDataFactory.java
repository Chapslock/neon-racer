package org.chapzlock.core.asset;

import java.util.ArrayList;
import java.util.List;

import lombok.experimental.UtilityClass;

/**
 * Utility class for generating simple Meshes
 */
@UtilityClass
public class RawMeshDataFactory {
    /**
     * Creates a 2D quad (rectangle) centered at origin in the XY plane.
     *
     * <p>Useful for sprites, UI elements, and screen-space effects. The quad lies
     * flat in the XY plane (Z=0) with normals implicitly facing +Z. Includes
     * UV coordinates mapped from (0,0) at bottom-left to (1,1) at top-right.
     *
     * @param width  total width of the quad along the X axis
     * @param height total height of the quad along the Y axis
     * @return mesh data for a single quad with 4 vertices and 2 triangles, ready for GPU upload
     */
    public static RawMeshData createQuad(float width, float height) {
        float hw = width / 2.0f;
        float hh = height / 2.0f;

        float[] positions = {
            -hw, -hh, 0.0f,
            hw, -hh, 0.0f,
            hw, hh, 0.0f,
            -hw, hh, 0.0f
        };

        float[] texCoords = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };

        int[] indices = {
            0, 1, 2,
            2, 3, 0
        };

        return new RawMeshData(positions, texCoords, indices);
    }


    /**
     * Creates an equilateral triangle centered at origin in the XY plane.
     *
     * <p>The triangle is oriented with one vertex pointing up (+Y) and two vertices
     * along the base. Lies flat in the XY plane (Z=0). Useful for simple geometric
     * primitives, particle effects, or placeholder meshes during development.
     *
     * @param size the length of each side of the equilateral triangle
     * @return mesh data for a single triangle with 3 vertices and 1 face, ready for GPU upload
     */
    public static RawMeshData createTriangle(float size) {
        float h = (float) (Math.sqrt(3) / 2 * size);

        float[] positions = {
            0.0f, h / 2, 0.0f,
            -size / 2, -h / 2, 0.0f,
            size / 2, -h / 2, 0.0f
        };

        float[] texCoords = {
            0.5f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
        };

        int[] indices = {0, 1, 2};

        return new RawMeshData(positions, texCoords, indices);
    }


    /**
     * Creates a 3D cube centered at origin with the given side length.
     *
     * <p>Each face has its own 4 vertices (no shared vertices between faces) for correct
     * per-face lighting and UV mapping. UV coordinates are mapped independently per face
     * covering the full 0-1 range. Produces 24 vertices (4 per face x 6 faces) and
     * 36 indices (2 triangles per face x 6 faces).
     *
     * @param size the length of each edge of the cube
     * @return mesh data for a cube with 24 vertices, ready for GPU upload
     */
    public static RawMeshData createCube(float size) {
        float h = size / 2.0f;
        float[] positions = {
            // Front face
            -h, -h, h, h, -h, h, h, h, h, -h, h, h,
            // Back face
            -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -h,
            // Left face
            -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h,
            // Right face
            h, -h, -h, h, h, -h, h, h, h, h, -h, h,
            // Top face
            -h, h, -h, -h, h, h, h, h, h, h, h, -h,
            // Bottom face
            -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h
        };

        float[] texCoords = {
            // Same texcoords for each face
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f
        };

        int[] indices = {
            0, 1, 2, 2, 3, 0, // Front
            4, 5, 6, 6, 7, 4, // Back
            8, 9, 10, 10, 11, 8, // Left
            12, 13, 14, 14, 15, 12, // Right
            16, 17, 18, 18, 19, 16, // Top
            20, 21, 22, 22, 23, 20 // Bottom
        };

        return new RawMeshData(positions, texCoords, indices);
    }


    /**
     * Creates a UV sphere centered at origin with configurable resolution.
     *
     * <p>Generated via latitude/longitude subdivision. Higher segment counts produce
     * smoother surfaces but more vertices. Includes UV coordinates mapped spherically
     * (U along longitude, V along latitude). Degenerate triangles at the poles are
     * automatically excluded from the index buffer.
     *
     * @param radius  the radius of the sphere
     * @param sectors number of vertical slices (longitude divisions); more sectors = smoother equator
     * @param stacks  number of horizontal rings (latitude divisions); more stacks = smoother poles
     * @return mesh data for a UV sphere, ready for GPU upload
     */
    public static RawMeshData createSphere(float radius, int sectors, int stacks) {
        List<Float> posList = new ArrayList<>();
        List<Float> texList = new ArrayList<>();
        List<Integer> idxList = new ArrayList<>();
        for (int i = 0; i <= stacks; i++) {
            float stackAngle = (float) Math.PI / 2 - i * (float) Math.PI / stacks;
            float xy = radius * (float) Math.cos(stackAngle);
            float z = radius * (float) Math.sin(stackAngle);

            for (int j = 0; j <= sectors; j++) {
                float sectorAngle = j * 2.0f * (float) Math.PI / sectors;
                float x = xy * (float) Math.cos(sectorAngle);
                float y = xy * (float) Math.sin(sectorAngle);
                posList.add(x);
                posList.add(y);
                posList.add(z);
                texList.add((float) j / sectors);
                texList.add((float) i / stacks);
            }
        }
        for (int i = 0; i < stacks; i++) {
            int k1 = i * (sectors + 1);
            int k2 = k1 + sectors + 1;
            for (int j = 0; j < sectors; j++, k1++, k2++) {
                if (i != 0) {
                    idxList.add(k1);
                    idxList.add(k2);
                    idxList.add(k1 + 1);
                }
                if (i != (stacks - 1)) {
                    idxList.add(k1 + 1);
                    idxList.add(k2);
                    idxList.add(k2 + 1);
                }
            }
        }

        float[] positions = new float[posList.size()];
        for (int i = 0; i < posList.size(); i++) {
            positions[i] = posList.get(i);
        }

        float[] texCoords = new float[texList.size()];
        for (int i = 0; i < texList.size(); i++) {
            texCoords[i] = texList.get(i);
        }

        int[] indices = new int[idxList.size()];
        for (int i = 0; i < idxList.size(); i++) {
            indices[i] = idxList.get(i);
        }
        return new RawMeshData(positions, texCoords, indices);
    }

    /**
     * Creates a unit quad for UI rendering with positions in the [0,1] range.
     *
     * <p>Unlike {@link #createQuad(float, float)} which is centered at origin, this quad
     * has its origin at the top-left corner with X and Y ranging from 0 to 1. This makes
     * it natural for screen-space UI layouts where transforms position the top-left corner.
     * Includes normals facing +Z and UV coordinates covering the full 0-1 range (flipped
     * vertically so texture origin is top-left).
     *
     * @return mesh data for a UI quad with 4 vertices, normals, and UVs, ready for GPU upload
     */
    public static RawMeshData createUi() {
        float[] positions = {
            0f, 0f, 0f,   // 0 top-left
            1f, 0f, 0f,   // 1 top-right
            1f, 1f, 0f,   // 2 bottom-right
            0f, 1f, 0f    // 3 bottom-left
        };

        float[] texCoords = {
            0f, 1f, // top-left -> (0,1)
            1f, 1f,
            1f, 0f,
            0f, 0f
        };

        float[] normals = {
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f
        };

        int[] indices = {
            0, 1, 2,
            2, 3, 0
        };

        return new RawMeshData(positions, texCoords, indices, normals);
    }
}
