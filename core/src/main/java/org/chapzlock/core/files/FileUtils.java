package org.chapzlock.core.files;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.geometry.RawMeshData;
import org.chapzlock.core.graphics.RawImageData;
import org.chapzlock.core.logging.Log;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FileUtils {

    /**
     * Loads a content of a file to String from the resources directory
     *
     * @param resourcePath path of the file relative to the resources directory
     * @return file content as string
     */
    public static String loadAsString(String resourcePath) {
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
     * Loads an Wavefront (.obj) file from the resource folder and creates RawMeshData from it
     *
     * @param resourcePath path of the file relative to the resource directory
     * @return RawMeshData for the given wavefront file
     */
    public static RawMeshData loadMeshData(String resourcePath) {
        try (BufferedReader br = openResource(resourcePath)) {
            return parseObjFile(br);
        } catch (Exception e) {
            Log.error("Error while reading wavefront file! error: " + e.getMessage());
            throw new RuntimeException("Failed to read file: " + resourcePath, e);
        }
    }

    /**
     * Loads raw image data from an image file
     *
     * @param resourcePath path of the file relative to the resource directory
     * @return raw image data
     */
    public static RawImageData loadImage(String resourcePath) {
        ByteBuffer image;
        int width;
        int height;

        try {
            ByteBuffer resourceBuffer = ioResourceToByteBuffer(resourcePath);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                STBImage.stbi_set_flip_vertically_on_load(true);
                image = STBImage.stbi_load_from_memory(resourceBuffer, w, h, comp, 4);
                if (image == null) {
                    throw new RuntimeException("Failed to load texture: " + STBImage.stbi_failure_reason());
                }
                width = w.get(0);
                height = h.get(0);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + resourcePath, e);
        }
        return new RawImageData(image, width, height, resourcePath);
    }

    /**
     * helper to read a resource into a ByteBuffer
     */
    private static ByteBuffer ioResourceToByteBuffer(String resourcePath) throws IOException {
        ByteBuffer buffer;
        Path path = Paths.get(resourcePath);
        if (Files.isReadable(path)) {
            try (SeekableByteChannel fc = Files.newByteChannel(path)) {
                buffer = ByteBuffer.allocateDirect((int) fc.size() + 1);
                while (fc.read(buffer) != -1)
                    ;
            }
        } else {
            try (
                var source = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
                var rbc = Channels.newChannel(source)
            ) {
                buffer = ByteBuffer.allocateDirect(8192);
                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1) {
                        break;
                    }
                    if (buffer.remaining() == 0) {
                        ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.capacity() * 2);
                        buffer.flip();
                        newBuffer.put(buffer);
                        buffer = newBuffer;
                    }
                }
            }
        }
        buffer.flip();
        return buffer;
    }

    private static BufferedReader openResource(String resourcePath) throws IOException {
        InputStream fileStream = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(resourcePath);

        if (fileStream == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        }
        return new BufferedReader(new InputStreamReader(fileStream));
    }

    private static RawMeshData parseObjFile(BufferedReader br) throws IOException {
        List<float[]> tempVertices = new ArrayList<>();
        List<float[]> tempTexCoords = new ArrayList<>();
        List<float[]> tempNormals = new ArrayList<>();

        List<float[]> outVertices = new ArrayList<>();
        List<float[]> outTexCoords = new ArrayList<>();
        List<float[]> outNormals = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();

        Map<String, Integer> uniqueMap = new HashMap<>();

        String line;
        while ((line = br.readLine()) != null) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            processObjLine(line, tempVertices, tempTexCoords, tempNormals,
                outVertices, outTexCoords, outNormals, indicesList, uniqueMap);
        }

        return new RawMeshData(
            flatten3f(outVertices),
            flatten2f(outTexCoords),
            indicesList.stream().mapToInt(i -> i).toArray(),
            flatten3f(outNormals)
        );
    }

    private static void processObjLine(
        String line,
        List<float[]> tempVertices,
        List<float[]> tempTexCoords,
        List<float[]> tempNormals,
        List<float[]> outVertices,
        List<float[]> outTexCoords,
        List<float[]> outNormals,
        List<Integer> indicesList,
        Map<String, Integer> uniqueMap
    ) {
        String[] tokens = line.split("\\s+");
        switch (tokens[0]) {
            case "v" -> tempVertices.add(parseFloatArray(tokens, 1, 3));
            case "vt" -> tempTexCoords.add(parseFloatArray(tokens, 1, 2));
            case "vn" -> tempNormals.add(parseFloatArray(tokens, 1, 3));
            case "f" -> processFace(tokens, tempVertices, tempTexCoords, tempNormals,
                outVertices, outTexCoords, outNormals, indicesList, uniqueMap);
            default -> {
            }
        }
    }

    private static float[] flatten3f(List<float[]> list) {
        float[] result = new float[list.size() * 3];
        for (int i = 0; i < list.size(); i++) {
            float[] arr = list.get(i);
            System.arraycopy(arr, 0, result, i * 3, 3);
        }
        return result;
    }

    private static float[] flatten2f(List<float[]> list) {
        float[] result = new float[list.size() * 2];
        for (int i = 0; i < list.size(); i++) {
            float[] arr = list.get(i);
            System.arraycopy(arr, 0, result, i * 2, 2);
        }
        return result;
    }

    private static float[] parseFloatArray(String[] tokens, int start, int count) {
        float[] result = new float[count];
        for (int i = 0; i < count; i++) {
            result[i] = Float.parseFloat(tokens[start + i]);
        }
        return result;
    }

    private static void processFace(
        String[] tokens,
        List<float[]> tempVertices,
        List<float[]> tempTexCoords,
        List<float[]> tempNormals,
        List<float[]> outVertices,
        List<float[]> outTexCoords,
        List<float[]> outNormals,
        List<Integer> indicesList,
        Map<String, Integer> uniqueMap
    ) {
        // OBJ faces are usually triangles, but may be quads or more.
        for (int i = 1; i < tokens.length; i++) {
            String key = tokens[i];
            Integer index = uniqueMap.get(key);

            if (index == null) {
                String[] parts = key.split("/");
                int vIndex = Integer.parseInt(parts[0]) - 1;
                int tIndex = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) - 1 : -1;
                int nIndex = parts.length > 2 ? Integer.parseInt(parts[2]) - 1 : -1;

                outVertices.add(tempVertices.get(vIndex));
                outTexCoords.add(tIndex >= 0 ? tempTexCoords.get(tIndex) : new float[]{0, 0});
                outNormals.add(nIndex >= 0 ? tempNormals.get(nIndex) : new float[]{0, 0, 0});

                index = outVertices.size() - 1;
                uniqueMap.put(key, index);
            }

            indicesList.add(index);
        }
    }

}
