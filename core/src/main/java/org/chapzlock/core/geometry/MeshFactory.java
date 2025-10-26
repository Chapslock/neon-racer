package org.chapzlock.core.geometry;

import java.util.ArrayList;
import java.util.List;

import lombok.experimental.UtilityClass;

/**
 * Utility class for generating simple Meshes
 */
@UtilityClass
public class MeshFactory {
    /**
     * 2D Quad (centered at origin)
     *
     * @param width
     * @param height
     * @return
     */
    public static MeshData createQuad(float width, float height) {
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

        return new MeshData(positions, texCoords, indices);
    }


    /**
     * 2D Triangle
     *
     * @param size
     * @return
     */
    public static MeshData createTriangle(float size) {
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

        return new MeshData(positions, texCoords, indices);
    }


    /**
     * 3D Cube
     *
     * @param size
     * @return
     */
    public static MeshData createCube(float size) {
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

        return new MeshData(positions, texCoords, indices);
    }


    /**
     * 3D Sphere (low-poly UV sphere)
     */
    public static MeshData createSphere(float radius, int sectors, int stacks) {
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
        return new MeshData(positions, texCoords, indices);
    }

    /**
     * Generates a flat Terrain mesh with the parameters
     *
     * @param vertexCount
     * @param size
     * @return
     */
    public static MeshData generateFlatTerrain(int vertexCount, float size) {
        int count = vertexCount * vertexCount;
        float[] vertices = new float[count * 3];
        float[] normals = new float[count * 3];
        float[] textureCoords = new float[count * 2];
        int[] indices = new int[6 * (vertexCount - 1) * (vertexCount - 1)];

        int vertexPointer = 0;
        for (int i = 0; i < vertexCount; i++) {
            for (int j = 0; j < vertexCount; j++) {
                // Vertex position
                vertices[vertexPointer * 3] = (float) j / ((float) vertexCount - 1) * size; // X
                vertices[vertexPointer * 3 + 1] = 0; // Y (heightmap later)
                vertices[vertexPointer * 3 + 2] = (float) i / ((float) vertexCount - 1) * size; // Z

                // Normal (flat up for now)
                normals[vertexPointer * 3] = 0;
                normals[vertexPointer * 3 + 1] = 1;
                normals[vertexPointer * 3 + 2] = 0;

                // Texture coordinates
                textureCoords[vertexPointer * 2] = (float) j / ((float) vertexCount - 1);
                textureCoords[vertexPointer * 2 + 1] = (float) i / ((float) vertexCount - 1);

                vertexPointer++;
            }
        }

        // Build indices for triangle rendering
        int pointer = 0;
        for (int gz = 0; gz < vertexCount - 1; gz++) {
            for (int gx = 0; gx < vertexCount - 1; gx++) {
                int topLeft = (gz * vertexCount) + gx;
                int topRight = topLeft + 1;
                int bottomLeft = ((gz + 1) * vertexCount) + gx;
                int bottomRight = bottomLeft + 1;

                // First triangle
                indices[pointer++] = topLeft;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = topRight;

                // Second triangle
                indices[pointer++] = topRight;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = bottomRight;
            }
        }
        return new MeshData(vertices, textureCoords, indices, normals);
    }
}
