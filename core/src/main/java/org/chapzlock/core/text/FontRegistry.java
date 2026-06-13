package org.chapzlock.core.text;

import org.chapzlock.core.application.Disposable;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.render.RefCounted;
import org.chapzlock.core.render.ResourceIdGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * Manages font lifecycle: caching, reference counting, and GPU cleanup.
 *
 * <p><strong>What this does:</strong> Loads TrueType fonts (.ttf) from resources, generates
 * MSDF atlases via {@link MsdfAtlasGenerator}, uploads the atlas texture to the GPU, and
 * provides reference-counted lifecycle management so fonts can be shared across scenes.
 *
 * <p><strong>Loading strategy:</strong> Fonts are loaded on-demand the first time
 * {@link #load(String)} is called. Subsequent calls return the cached instance. Use
 * {@link #retain(String)} / {@link #release(String)} for scene-based reference counting.
 *
 * <p><strong>GPU cleanup:</strong> When a font's reference count drops to zero, its GPU
 * atlas texture is automatically freed. On engine shutdown, {@link #dispose()} frees all
 * remaining GPU resources unconditionally.
 *
 * @see Font
 * @see MsdfAtlasGenerator
 * @see RefCounted
 */
public class FontRegistry implements Disposable {

    private static final Log log = Log.create(FontRegistry.class);

    private final Map<String, Font> fontCache = new HashMap<>();
    private final Map<String, Integer> pathToId = new HashMap<>();
    private final Map<Integer, String> idToPath = new HashMap<>();
    private final RefCounted refCounted = new RefCounted(this::deleteFont);

    /**
     * Load a font from the classpath, generating an MSDF atlas if not already cached.
     *
     * <p>The font is loaded at the specified pixel height (used for metrics scaling).
     * The resource path should be relative to src/main/resources/ (e.g., "fonts/Roboto-Regular.ttf").
     *
     * @param resourcePath the path to the .ttf font file in resources
     * @param pixelHeight the target pixel height for the font (default 32 if you want crisp text at medium sizes)
     * @return the loaded Font with GPU atlas and metrics
     * @throws RuntimeException if the font file cannot be loaded or atlas generation fails
     */
    public Font load(String resourcePath, float pixelHeight) {
        if (fontCache.containsKey(resourcePath)) {
            return fontCache.get(resourcePath);
        }

        log.info("Loading font: %s (pixelHeight=%.1f)", resourcePath, pixelHeight);

        ByteBuffer ttfData = loadResourceToByteBuffer(resourcePath);
        RawFontAtlas atlas = MsdfAtlasGenerator.generate(ttfData, pixelHeight);

        Texture atlasTexture = uploadAtlas(atlas, resourcePath);

        Font font = new Font(
                atlasTexture,
                atlas.getGlyphs(),
                atlas.getKerning(),
                atlas.getLineHeight(),
                atlas.getAscent(),
                atlas.getDescent(),
                atlas.getDistanceRange()
        );

        int id = ResourceIdGenerator.nextId();
        fontCache.put(resourcePath, font);
        pathToId.put(resourcePath, id);
        idToPath.put(id, resourcePath);

        log.info("Font loaded: %s (atlas=%dx%d, glyphs=%d)",
                resourcePath, atlas.getAtlasWidth(), atlas.getAtlasHeight(), atlas.getGlyphs().size());

        return font;
    }

    /**
     * Load a font with default pixel height of 32.
     *
     * @param resourcePath the path to the .ttf font file in resources
     * @return the loaded Font
     * @see #load(String, float)
     */
    public Font load(String resourcePath) {
        return load(resourcePath, 32f);
    }

    /**
     * Get a previously loaded font from the cache.
     *
     * @param resourcePath the path to the .ttf font file
     * @return the cached Font
     * @throws IllegalStateException if the font has not been loaded yet
     */
    public Font get(String resourcePath) {
        Font font = fontCache.get(resourcePath);
        if (font == null) {
            throw new IllegalStateException("Font not loaded: " + resourcePath +
                    ". Call load() before get().");
        }
        return font;
    }

    /**
     * Increment the reference count for a font.
     *
     * <p>Called by {@link org.chapzlock.core.scene.SceneManager} when a scene using this
     * font is instantiated. Prevents the font from being freed while in use.
     *
     * @param resourcePath the font resource path to retain
     */
    public void retain(String resourcePath) {
        Integer id = pathToId.get(resourcePath);
        if (id != null) {
            refCounted.retain(id);
        }
    }

    /**
     * Decrement the reference count for a font.
     *
     * <p>Called by {@link org.chapzlock.core.scene.SceneManager} when a scene using this
     * font is unloaded. When the count reaches zero, the GPU atlas texture is freed.
     *
     * @param resourcePath the font resource path to release
     */
    public void release(String resourcePath) {
        Integer id = pathToId.get(resourcePath);
        if (id != null) {
            refCounted.release(id);
        }
    }

    /**
     * Free all GPU atlas textures and clear the cache.
     *
     * <p>Called by {@link org.chapzlock.core.application.Engine#dispose()} on shutdown.
     * Ignores reference counts and unconditionally frees everything.
     */
    @Override
    public void dispose() {
        log.info("Disposing %d cached fonts", fontCache.size());
        for (Font font : fontCache.values()) {
            if (font.getAtlasTexture() != null) {
                glDeleteTextures(font.getAtlasTexture().getTextureHandle());
            }
        }
        fontCache.clear();
        pathToId.clear();
        idToPath.clear();
    }

    /**
     * Upload an MSDF atlas to the GPU as an RGB8 texture.
     *
     * <p>Uses GL_LINEAR filtering for smooth rendering and GL_CLAMP_TO_EDGE to prevent
     * texture seams at atlas boundaries.
     *
     * @param atlas the CPU-side atlas with pixel data
     * @param path the font resource path (used for Texture.filePath)
     * @return a Texture component wrapping the GPU handle
     */
    private Texture uploadAtlas(RawFontAtlas atlas, String path) {
        int handle = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, handle);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8,
                atlas.getAtlasWidth(), atlas.getAtlasHeight(), 0,
                GL_RGB, GL_UNSIGNED_BYTE, atlas.getPixelData());

        glBindTexture(GL_TEXTURE_2D, 0);

        return Texture.builder()
                .id(ResourceIdGenerator.nextId())
                .textureHandle(handle)
                .width(atlas.getAtlasWidth())
                .height(atlas.getAtlasHeight())
                .filePath(path)
                .build();
    }

    /**
     * Callback invoked by RefCounted when a font's reference count reaches zero.
     *
     * <p>Frees the GPU atlas texture and removes the font from the cache.
     *
     * @param id the internal resource ID for the font
     */
    private void deleteFont(int id) {
        String path = idToPath.remove(id);
        if (path != null) {
            Font font = fontCache.remove(path);
            pathToId.remove(path);
            if (font != null && font.getAtlasTexture() != null) {
                glDeleteTextures(font.getAtlasTexture().getTextureHandle());
                log.info("Released font: %s", path);
            }
        }
    }

    /**
     * Load a resource file into a direct ByteBuffer.
     *
     * <p>Adapted from {@link org.chapzlock.core.asset.FileUtils#ioResourceToByteBuffer}.
     * First tries filesystem path, then falls back to classpath resource.
     *
     * @param resourcePath the path to the resource file
     * @return a direct ByteBuffer containing the file bytes
     * @throws RuntimeException if the resource cannot be found or read
     */
    private ByteBuffer loadResourceToByteBuffer(String resourcePath) {
        try {
            // Try filesystem first
            Path path = Paths.get(resourcePath);
            if (Files.isReadable(path)) {
                return ByteBuffer.wrap(Files.readAllBytes(path));
            }

            // Fall back to classpath resource
            try (InputStream source = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(resourcePath)) {
                if (source == null) {
                    throw new IOException("Resource not found on classpath: \"" + resourcePath + "\". " +
                            "Ensure the file exists in src/main/resources/ and the path is relative to the resources root.");
                }

                try (var rbc = Channels.newChannel(source)) {
                    ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
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
                    buffer.flip();
                    return buffer;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font resource: " + resourcePath, e);
        }
    }
}
