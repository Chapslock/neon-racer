package org.chapzlock.core.geometry;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MeshFactory {
    // 2D Quad (centered at origin)
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


    // 2D Triangle
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


    // 3D Cube
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
            8, 9,10, 10,11, 8, // Left
            12,13,14, 14,15,12, // Right
            16,17,18, 18,19,16, // Top
            20,21,22, 22,23,20 // Bottom
        };


        return new MeshData(positions, texCoords, indices);
    }


    // 3D Sphere (low-poly UV sphere)
    public static MeshData createSphere(float radius, int sectors, int stacks) {
        java.util.List<Float> posList = new java.util.ArrayList<>();
        java.util.List<Float> texList = new java.util.ArrayList<>();
        java.util.List<Integer> idxList = new java.util.ArrayList<>();


        for (int i = 0; i <= stacks; i++) {
            float stackAngle = (float) Math.PI / 2 - (float) i * (float) Math.PI / stacks;
            float xy = radius * (float) Math.cos(stackAngle);
            float z = radius * (float) Math.sin(stackAngle);


            for (int j = 0; j <= sectors; j++) {
                float sectorAngle = (float) j * 2.0f * (float) Math.PI / sectors;
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
        for (int i = 0; i < posList.size(); i++) positions[i] = posList.get(i);


        float[] texCoords = new float[texList.size()];
        for (int i = 0; i < texList.size(); i++) texCoords[i] = texList.get(i);


        int[] indices = new int[idxList.size()];
        for (int i = 0; i < idxList.size(); i++) indices[i] = idxList.get(i);


        return new MeshData(positions, texCoords, indices);
    }
}
