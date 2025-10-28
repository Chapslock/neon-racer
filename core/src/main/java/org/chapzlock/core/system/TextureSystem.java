package org.chapzlock.core.system;

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
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.chapzlock.core.component.Texture;
import org.lwjgl.opengl.GL13;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class TextureSystem {

    /**
     * Load a texture from a resource path and create a Texture (calls GL).
     */
    public Texture loadTexture(String resourcePath) {
        ByteBuffer image;
        int width, height;

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

        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
        glBindTexture(GL_TEXTURE_2D, 0);

        STBImage.stbi_image_free(image);
        return new Texture(id, width, height, resourcePath);
    }

    /**
     * helper to read a resource into a ByteBuffer
     */
    private static ByteBuffer ioResourceToByteBuffer(String resourcePath) throws IOException {
        ByteBuffer buffer;
        Path path = Paths.get(resourcePath);
        if (java.nio.file.Files.isReadable(path)) {
            try (SeekableByteChannel fc = java.nio.file.Files.newByteChannel(path)) {
                buffer = java.nio.ByteBuffer.allocateDirect((int) fc.size() + 1);
                while (fc.read(buffer) != -1);
            }
        } else {
            try (
                var source = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
                var rbc = Channels.newChannel(source)
            ) {
                buffer = java.nio.ByteBuffer.allocateDirect(8192);
                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1) {
                        break;
                    }
                    if (buffer.remaining() == 0) {
                        ByteBuffer newBuffer = java.nio.ByteBuffer.allocateDirect(buffer.capacity() * 2);
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

    public void bind(Texture tex, int unit) {
        if (tex == null) {
            return;
        }
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, tex.getHandle());
    }

    public void unbind(Texture tex) {
        if (tex == null) {
            return;
        }
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void delete(Texture tex) {
        if (tex == null) {
            return;
        }
        glDeleteTextures(tex.getHandle());
    }
}
