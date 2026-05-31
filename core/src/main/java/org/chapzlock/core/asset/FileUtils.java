package org.chapzlock.core.asset;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
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

import javax.imageio.ImageIO;

import org.chapzlock.core.render.RawImageData;
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
     * Loads raw image data from an image file
     *
     * @param resourcePath path of the file relative to the resource directory
     * @return raw image data
     */
    public static RawImageData loadRawImage(String resourcePath) {
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
     * Loads an image from the classpath resource directory (e.g., src/main/resources).
     *
     * @param resourcePath The path to the image resource (e.g. "textures/heightmap.png").
     * @return The loaded BufferedImage.
     * @throws RuntimeException if the resource cannot be found or read.
     */
    public static BufferedImage loadBufferedImage(String resourcePath) {
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        try (InputStream stream = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load image resource: " + resourcePath, e);
        }
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

}
