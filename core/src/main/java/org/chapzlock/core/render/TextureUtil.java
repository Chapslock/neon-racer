package org.chapzlock.core.render;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8;
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
 * Low-level OpenGL calls for texture resource management.
 *
 * <p><strong>What is a texture?</strong> A texture is an image stored on the GPU that gets
 * "painted" onto 3D geometry during rendering. The fragment shader samples (reads) colors
 * from the texture using UV coordinates that map each triangle's surface to a region of
 * the image.
 *
 * <p><strong>Texture lifecycle:</strong>
 * <ol>
 *   <li><em>Create + Upload</em> — {@link #bindTextureDataToGpu} allocates a GPU texture
 *       object, uploads pixel data from CPU memory, generates mipmaps, then frees the CPU
 *       copy.</li>
 *   <li><em>Bind</em> — {@link #bind} activates a texture on a numbered "texture unit" so
 *       the shader can sample from it. Multiple textures can be bound simultaneously on
 *       different units (e.g., unit 0 = base color, unit 1 = normal map).</li>
 *   <li><em>Delete</em> — {@link #delete} frees the GPU memory when the texture is no
 *       longer needed.</li>
 * </ol>
 *
 * <p><strong>What are texture units?</strong> The GPU has numbered "slots" (GL_TEXTURE0,
 * GL_TEXTURE1, etc.) that each hold one active texture. A shader's sampler uniforms reference
 * these slot numbers, not texture IDs directly. This allows binding multiple textures for a
 * single draw call (base color on unit 0, metallic-roughness on unit 1, etc.).
 *
 * <p><strong>What are mipmaps?</strong> Pre-computed smaller versions of the texture (half
 * size, quarter size, etc.) used when an object is far from the camera. Without mipmaps,
 * distant textures flicker (aliasing). The GPU automatically picks the appropriate mipmap
 * level based on distance.
 *
 * <p><strong>What is sRGB?</strong> Color textures (base color, emissive) are typically
 * authored in sRGB color space (what monitors display). The GPU must convert from sRGB to
 * linear for correct lighting math. Using {@code GL_SRGB8_ALPHA8} as the internal format
 * tells OpenGL to do this conversion automatically when the shader samples the texture.
 * Non-color textures (normal maps, roughness) should NOT use sRGB — they contain raw data,
 * not perceptual colors.
 *
 * @see TextureSystem
 */
@UtilityClass
public class TextureUtil {

    /**
     * Uploads decoded RGBA pixel data to the GPU and creates a texture object.
     *
     * <p>Configures the texture with:
     * <ul>
     *   <li>Wrapping: {@code GL_REPEAT} — texture tiles when UV coordinates exceed [0,1]</li>
     *   <li>Min filter: {@code GL_LINEAR_MIPMAP_LINEAR} — trilinear filtering (smooth at all distances)</li>
     *   <li>Mag filter: {@code GL_LINEAR} — bilinear filtering when zoomed in</li>
     *   <li>Mipmaps: auto-generated from the uploaded image</li>
     * </ul>
     *
     * <p>After upload, the CPU-side pixel buffer is freed via
     * {@link RawImageData#deleteBufferFromMemory()}.
     *
     * @param rawImageData decoded RGBA pixel data with dimensions
     * @return a Texture component with GPU handle and metadata, ready for rendering
     */
    public static Texture bindTextureDataToGpu(RawImageData rawImageData) {
        return bindTextureDataToGpu(rawImageData, false);
    }

    /**
     * Uploads decoded RGBA pixel data to the GPU with optional sRGB color space handling.
     *
     * <p>When {@code srgb} is true, the internal format is {@code GL_SRGB8_ALPHA8}, which
     * tells the GPU to automatically linearize color values when the shader samples the
     * texture. Use sRGB for color textures (base color, emissive). Do NOT use sRGB for
     * data textures (normal maps, metallic-roughness, occlusion) — they contain raw
     * numeric data, not perceptual colors.
     *
     * @param rawImageData decoded RGBA pixel data with dimensions
     * @param srgb true to store as sRGB (auto-linearized on sample), false for linear/raw data
     * @return a Texture component with GPU handle and metadata, ready for rendering
     */
    public static Texture bindTextureDataToGpu(RawImageData rawImageData, boolean srgb) {
        int handle = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, handle);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        int internalFormat = srgb ? GL_SRGB8_ALPHA8 : GL_RGBA8;
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat,
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
     * Binds a texture to a numbered texture unit for rendering.
     *
     * <p>After this call, any shader sampler uniform set to {@code unit} will read from
     * this texture. For example, if the PBR shader's "uBaseColorMap" sampler is set to
     * unit 0, calling {@code bind(myTexture, 0)} makes the shader sample from myTexture.
     *
     * <p>Multiple textures can be bound simultaneously on different units. The PBR renderer
     * uses units 0-4 for base color, metallic-roughness, normal, occlusion, and emissive.
     *
     * @param texture the texture to bind
     * @param unit the texture unit index (0 = GL_TEXTURE0, 1 = GL_TEXTURE1, etc.)
     */
    public static void bind(Texture texture, int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, texture.getTextureHandle());
    }

    /**
     * Unbinds the currently bound 2D texture from the active texture unit.
     *
     * <p>Called after rendering to ensure no texture accidentally bleeds into the next
     * draw call or render pass.
     */
    public static void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Deletes a texture from GPU memory.
     *
     * <p>After this call, the texture's OpenGL handle is invalid. Rendering with a deleted
     * texture produces undefined behavior (usually a black or white surface).
     *
     * @param texture the texture to free
     */
    public static void delete(Texture texture) {
        glDeleteTextures(texture.getTextureHandle());
    }
}
