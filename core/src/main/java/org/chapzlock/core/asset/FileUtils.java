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

/**
 * Utility methods for loading files and images from the classpath or filesystem.
 *
 * <p><strong>Resource loading strategy:</strong> All game assets (shaders, textures, models)
 * live in {@code src/main/resources/}. At runtime, they're accessed via the classloader which
 * packages them inside the JAR. The methods here abstract over this — you pass a path relative
 * to the resources root (e.g., "shaders/pbr.vert") and get back the file contents.
 *
 * <p><strong>Image loading:</strong> Uses the STB library (via LWJGL) to decode PNG/JPEG/BMP
 * into raw RGBA pixel buffers suitable for GPU upload. STB is a single-header C library that
 * LWJGL wraps — it's fast and supports all common formats.
 *
 * <p><strong>Thread safety:</strong> Image loading methods use a global STB flip setting
 * ({@code stbi_set_flip_vertically_on_load}). All image loading MUST happen on the main
 * thread. Do not load images from background threads — the global state will corrupt.
 *
 * @see org.chapzlock.core.render.TextureSystem
 * @see org.chapzlock.core.render.RawImageData
 */
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
        return loadRawImage(resourcePath, true);
    }

    public static RawImageData loadRawImage(String resourcePath, boolean flipVertically) {
        ByteBuffer image;
        int width;
        int height;

        try {
            ByteBuffer resourceBuffer = ioResourceToByteBuffer(resourcePath);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                STBImage.stbi_set_flip_vertically_on_load(flipVertically);
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
     * Decodes raw image bytes (PNG/JPEG) from a ByteBuffer into RGBA pixel data.
     *
     * <p>Used for embedded GLB textures where the image data is already in memory
     * rather than on the filesystem. Uses STB to decode the compressed image format
     * into raw RGBA pixels suitable for GPU upload.
     *
     * @param imageBytes buffer containing encoded PNG/JPEG bytes
     * @param cacheKey   identifier for error messages and caching
     * @return decoded RGBA pixel data ready for GPU upload
     */
    public static RawImageData loadRawImageFromMemory(ByteBuffer imageBytes, String cacheKey) {
        return loadRawImageFromMemory(imageBytes, cacheKey, true);
    }

    public static RawImageData loadRawImageFromMemory(ByteBuffer imageBytes, String cacheKey, boolean flipVertically) {
        ByteBuffer directBuf = imageBytes;
        if (!imageBytes.isDirect()) {
            directBuf = ByteBuffer.allocateDirect(imageBytes.remaining());
            directBuf.put(imageBytes.duplicate());
            directBuf.flip();
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(flipVertically);
            ByteBuffer image = STBImage.stbi_load_from_memory(directBuf, w, h, comp, 4);
            if (image == null) {
                throw new RuntimeException("Failed to decode embedded texture '" + cacheKey + "': " + STBImage.stbi_failure_reason());
            }
            return new RawImageData(image, w.get(0), h.get(0), cacheKey);
        }
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
