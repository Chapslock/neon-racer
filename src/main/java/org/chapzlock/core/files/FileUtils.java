package org.chapzlock.core.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.chapzlock.core.geometry.MeshData;

public class FileUtils {

    /**
     * Loads a content of a file to String from the resources directory
     *
     * @param resourcePath path of the file relative to the resources directory
     * @return file content as string
     */
    public static String loadFileAsString(String resourcePath) {
        StringBuilder builder = new StringBuilder();

        try (
            InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath)
        ) {
            assert in != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))
            ) {

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read file! FileUtils name: " + resourcePath
                + System.lineSeparator() + ex.getMessage(), ex);
        }

        return builder.toString();
    }

    /**
     * Loads an Wavefront (.obj) file from the resource folder and creates MeshData from it
     *
     * @param resourcePath path of the file relative to the resource directory
     * @return MeshData for the given wavefront file
     */
    public static MeshData loadWavefrontFileToMesh(String resourcePath) {
        List<float[]> tempVertices = new ArrayList<>();
        List<float[]> tempTexCoords = new ArrayList<>();
        List<float[]> tempNormals = new ArrayList<>();

        List<Integer> indicesList = new ArrayList<>();
        List<float[]> outVertices = new ArrayList<>();
        List<float[]> outTexCoords = new ArrayList<>();
        List<float[]> outNormals = new ArrayList<>();

        Map<String, Integer> uniqueMap = new HashMap<>();

        InputStream fileStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(fileStream)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                switch (tokens[0]) {
                    case "v":
                        tempVertices.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                        });
                        break;
                    case "vt":
                        tempTexCoords.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2])
                        });
                        break;
                    case "vn":
                        tempNormals.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                        });
                        break;
                    case "f":
                        for (int i = 1; i <= 3; i++) {
                            String[] parts = tokens[i].split("/");
                            String key = String.join("/", parts);

                            if (!uniqueMap.containsKey(key)) {
                                int vIndex = Integer.parseInt(parts[0]) - 1;
                                int tIndex = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) - 1 : -1;
                                int nIndex = parts.length > 2 ? Integer.parseInt(parts[2]) - 1 : -1;

                                outVertices.add(tempVertices.get(vIndex));
                                outTexCoords.add(tIndex != -1 ? tempTexCoords.get(tIndex) : new float[]{0, 0});
                                outNormals.add(nIndex != -1 ? tempNormals.get(nIndex) : new float[]{0, 0, 0});

                                uniqueMap.put(key, outVertices.size() - 1);
                            }
                            indicesList.add(uniqueMap.get(key));
                        }
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error while reading wavefront file! error: " + e.getMessage());
            throw new RuntimeException("Failed to read file! File name: " + resourcePath
                + System.lineSeparator() + e.getMessage(), e);
        }

        // Flatten vertices
        float[] vertices = new float[outVertices.size() * 3];
        for (int i = 0; i < outVertices.size(); i++) {
            float[] v = outVertices.get(i);
            vertices[i * 3] = v[0];
            vertices[i * 3 + 1] = v[1];
            vertices[i * 3 + 2] = v[2];
        }

        // Flatten texCoords
        float[] texCoords = new float[outTexCoords.size() * 2];
        for (int i = 0; i < outTexCoords.size(); i++) {
            float[] t = outTexCoords.get(i);
            texCoords[i * 2] = t[0];
            texCoords[i * 2 + 1] = t[1];
        }

        // Flatten normals
        float[] normals = new float[outNormals.size() * 3];
        for (int i = 0; i < outNormals.size(); i++) {
            float[] n = outNormals.get(i);
            normals[i * 3] = n[0];
            normals[i * 3 + 1] = n[1];
            normals[i * 3 + 2] = n[2];
        }

        // Indices
        int[] indices = indicesList.stream().mapToInt(i -> i).toArray();

        return new MeshData(
            vertices,
            texCoords,
            indices,
            normals
        );
    }

}
