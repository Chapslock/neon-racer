package org.chapzlock.core.graphics;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.chapzlock.core.component.Component;
import org.lwjgl.system.MemoryStack;

/**
 * This class represents an OpenGL texture.
 *
 */
public class Texture implements Component {

    private final int id;
    private int width;
    private int height;

    public Texture() {
        id = glGenTextures();
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void setParameter(int name, int value) {
        glTexParameteri(GL_TEXTURE_2D, name, value);
    }

    public void delete() {
        glDeleteTextures(id);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static Texture loadTexture(String resourcePath) {
        ByteBuffer image;
        int width, height;

        try {
            ByteBuffer resourceBuffer = ioResourceToByteBuffer(resourcePath, 8192);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                stbi_set_flip_vertically_on_load(true);
                image = stbi_load_from_memory(resourceBuffer, w, h, comp, 4);
                if (image == null) {
                    throw new RuntimeException("Failed to load texture: " + stbi_failure_reason());
                }
                width = w.get(0);
                height = h.get(0);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + resourcePath, e);
        }

        return createTexture(width, height, image);
    }

    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        Path path = Paths.get(resource);
        if (java.nio.file.Files.isReadable(path)) {
            try (SeekableByteChannel fc = java.nio.file.Files.newByteChannel(path)) {
                buffer = ByteBuffer.allocateDirect((int) fc.size() + 1);
                while (fc.read(buffer) != -1);
            }
        } else {
            try (
                var source = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
                var rbc = Channels.newChannel(source)
            ) {
                buffer = ByteBuffer.allocateDirect(bufferSize);
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

    public static Texture createTexture(int width, int height, ByteBuffer data) {
        Texture texture = new Texture();

        texture.bind();
        texture.setParameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        texture.setParameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        texture.setParameter(GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        texture.setParameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        texture.uploadData(width, height, data);
        texture.unbind();

        return texture;
    }

    public void uploadData(int width, int height, ByteBuffer data) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        this.width = width;
        this.height = height;
    }
}
