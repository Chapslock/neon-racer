package org.chapzlock.core.graphics;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
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
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import org.chapzlock.core.component.Texture;

import lombok.experimental.UtilityClass;

/**
 * Handles OpenGL calls related to texture resources
 */
@UtilityClass
public class TextureUtil {

    public static Texture bindTextureDataToGpu(RawImageData rawImageData) {
        int handle = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, handle);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8,
            rawImageData.getWidth(), rawImageData.getHeight(), 0,
            GL_RGBA, GL_UNSIGNED_BYTE, rawImageData.getImageData());

        glGenerateMipmap(GL_TEXTURE_2D);

        glBindTexture(GL_TEXTURE_2D, 0);
        rawImageData.deleteBufferFromMemory();

        return Texture.builder()
            .id(ResourceIdGenerator.nextId())
            .textureHandle(handle)
            .height(rawImageData.getHeight())
            .width(rawImageData.getWidth())
            .filePath(rawImageData.getFilePath())
            .build();
    }

    /**
     * Binds a texture for rendering
     *
     * @param texture
     * @param unit    selects openGL texture unit
     */
    public static void bind(Texture texture, int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, texture.getTextureHandle());
    }

    public static void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static void delete(Texture texture) {
        glDeleteTextures(texture.getTextureHandle());
    }
}
