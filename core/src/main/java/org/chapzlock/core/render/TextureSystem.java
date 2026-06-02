package org.chapzlock.core.render;

import java.util.Map;

import org.chapzlock.core.application.Disposable;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.asset.FileUtils;
import org.chapzlock.core.logging.Log;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Loads, caches, and manages GPU texture resources.
 *
 * <p><strong>What this system does:</strong>
 * <ul>
 *   <li><em>Load from file:</em> Reads an image file (PNG/JPEG) from the classpath, decodes
 *       it into RGBA pixels, and uploads to the GPU via {@link TextureUtil}.</li>
 *   <li><em>Load from memory:</em> Decodes image bytes already in memory (used for embedded
 *       GLB textures that live in the binary chunk rather than as separate files).</li>
 *   <li><em>Deduplicate:</em> Same path (or cache key) loaded twice returns the cached texture
 *       instead of re-uploading. This saves GPU memory and avoids redundant disk I/O.</li>
 *   <li><em>Bind/Unbind:</em> Activates a texture on a numbered texture unit for rendering.</li>
 *   <li><em>Lifecycle:</em> Reference counting ensures textures are freed only when no scene
 *       uses them anymore.</li>
 * </ul>
 *
 * <p><strong>Cache key format:</strong> File-based textures are keyed by
 * {@code "path/to/file.png:srgb"} (or without ":srgb" for linear). Memory-based textures
 * are keyed by their provided cache key plus the sRGB suffix. This means the same image loaded
 * as sRGB and as linear will produce two separate GPU textures (correct behavior — they need
 * different internal formats).
 *
 * <p><strong>Reference counting:</strong> When a scene is loaded, SceneManager calls
 * {@link #retain(int)} for each texture used by that scene. When the scene is unloaded,
 * {@link #release(int)} is called. The texture is deleted from GPU memory only when its
 * reference count drops to zero.
 *
 * @see Texture
 * @see TextureUtil
 */
public class TextureSystem implements Disposable {

    /** All loaded textures, indexed by their engine-assigned ID. */
    private final Map<Integer, Texture> texturePool = new Int2ObjectOpenHashMap<>();

    /** Deduplication map: cache key → texture ID. Prevents loading the same image twice. */
    private final Map<String, Integer> filePathToIdMap = new Object2IntOpenHashMap<>();

    /**
     * Reference counting tracker for texture lifecycle.
     *
     * <p>When the reference count for a texture ID drops to zero (no scenes use it anymore),
     * the {@link #deleteTexture(int)} callback is fired to clean up GPU resources.
     */
    private final RefCounted refCounted = new RefCounted(this::deleteTexture);

    public TextureSystem() {
    }

    /**
     * Loads a texture from a classpath resource with default settings (no sRGB, flip vertically).
     *
     * <p>Vertical flip matches OpenGL's UV convention where (0,0) is bottom-left. This is the
     * standard for sprites and UI elements. For glTF textures, use the 3-parameter overload
     * with {@code flipVertically=false} since glTF uses top-left origin.
     *
     * @param filePath path relative to the resources root (e.g., "textures/player.png")
     * @return a GPU-bound Texture component, ready for rendering
     */
    public Texture load(String filePath) {
        return load(filePath, false, true);
    }

    /**
     * Loads a texture with sRGB color space option (flips vertically by default).
     *
     * @param filePath path relative to the resources root
     * @param srgb true for color textures (base color, emissive), false for data textures (normal, roughness)
     * @return a GPU-bound Texture component
     */
    public Texture load(String filePath, boolean srgb) {
        return load(filePath, srgb, true);
    }

    /**
     * Loads a texture with full control over sRGB and flip settings.
     *
     * <p>Returns a cached texture if this exact (path + sRGB flag) combination was already loaded.
     *
     * @param filePath path relative to the resources root
     * @param srgb true for sRGB internal format (auto-linearizes on sample), false for linear
     * @param flipVertically true to flip the image vertically (OpenGL UV convention)
     * @return a GPU-bound Texture component
     */
    public Texture load(String filePath, boolean srgb, boolean flipVertically) {
        String key = filePath + (srgb ? ":srgb" : "");
        Integer existingId = filePathToIdMap.get(key);
        if (existingId != null) {
            return texturePool.get(existingId);
        }
        RawImageData rawImageData = FileUtils.loadRawImage(filePath, flipVertically);
        Texture texture = TextureUtil.bindTextureDataToGpu(rawImageData, srgb);
        texturePool.put(texture.getId(), texture);
        filePathToIdMap.put(key, texture.getId());
        Log.debug("Texture loaded: \"" + filePath + "\" → id=" + texture.getId() + " (" + rawImageData.getWidth() + "x" + rawImageData.getHeight() + (srgb ? ", sRGB" : "") + ")");
        return texture;
    }

    /**
     * Loads a texture from in-memory image bytes (PNG/JPEG) with default settings.
     *
     * <p>Used for embedded GLB textures where the image data lives in the binary chunk
     * rather than as a separate file on disk.
     *
     * @param imageBytes raw PNG/JPEG bytes (must be a direct or heap ByteBuffer)
     * @param cacheKey unique identifier for deduplication (e.g., "embedded:model.glb:img0")
     * @return a GPU-bound Texture component
     */
    public Texture loadFromMemory(java.nio.ByteBuffer imageBytes, String cacheKey) {
        return loadFromMemory(imageBytes, cacheKey, false, true);
    }

    /**
     * Loads a texture from memory with sRGB option.
     *
     * @param imageBytes raw PNG/JPEG bytes
     * @param cacheKey unique identifier for deduplication
     * @param srgb true for color textures, false for data textures
     * @return a GPU-bound Texture component
     */
    public Texture loadFromMemory(java.nio.ByteBuffer imageBytes, String cacheKey, boolean srgb) {
        return loadFromMemory(imageBytes, cacheKey, srgb, true);
    }

    /**
     * Loads a texture from memory with full control over settings.
     *
     * <p>Returns a cached texture if this exact (cacheKey + sRGB flag) combination was
     * already loaded — avoids redundant decoding and GPU uploads when the same embedded
     * texture is referenced by multiple materials in a GLB file.
     *
     * @param imageBytes raw PNG/JPEG bytes
     * @param cacheKey unique identifier for deduplication
     * @param srgb true for sRGB internal format, false for linear
     * @param flipVertically true to flip the image vertically
     * @return a GPU-bound Texture component
     */
    public Texture loadFromMemory(java.nio.ByteBuffer imageBytes, String cacheKey, boolean srgb, boolean flipVertically) {
        String key = cacheKey + (srgb ? ":srgb" : "");
        Integer existingId = filePathToIdMap.get(key);
        if (existingId != null) {
            return texturePool.get(existingId);
        }
        RawImageData rawImageData = FileUtils.loadRawImageFromMemory(imageBytes, cacheKey, flipVertically);
        Texture texture = TextureUtil.bindTextureDataToGpu(rawImageData, srgb);
        texturePool.put(texture.getId(), texture);
        filePathToIdMap.put(key, texture.getId());
        Log.debug("Texture loaded from memory: \"" + cacheKey + "\" → id=" + texture.getId() + " (" + rawImageData.getWidth() + "x" + rawImageData.getHeight() + (srgb ? ", sRGB" : "") + ")");
        return texture;
    }

    /**
     * Binds a texture to a numbered texture unit for rendering.
     *
     * <p>After this call, any shader sampler uniform set to {@code unit} will sample
     * from this texture. Does nothing if texture is null (convenience for optional texture slots).
     *
     * @param texture the texture to bind (may be null — no-op)
     * @param unit the texture unit index (0, 1, 2, etc.)
     */
    public void bind(Texture texture, int unit) {
        if (texture == null) {
            return;
        }
        TextureUtil.bind(texture, unit);
    }

    /**
     * Unbinds the current texture from the active texture unit.
     *
     * <p>Does nothing if texture is null (convenience for optional texture slots).
     *
     * @param texture the texture to unbind (may be null — no-op)
     */
    public void unbind(Texture texture) {
        if (texture == null) {
            return;
        }
        TextureUtil.unbind();
    }

    /**
     * Immediately deletes a texture from GPU memory and removes it from the cache.
     *
     * <p>This bypasses reference counting — use only for textures that are NOT managed
     * by the scene system (e.g., temporary render targets).
     *
     * @param texture the texture to delete (may be null — no-op)
     */
    public void delete(Texture texture) {
        if (texture == null) {
            return;
        }
        TextureUtil.delete(texture);
        texturePool.remove(texture.getId());
    }

    /**
     * Increments the reference count for a texture.
     *
     * <p>Called by SceneManager when a scene using this texture is loaded.
     *
     * @param assetId the engine-assigned texture ID
     */
    public void retain(int assetId) {
        refCounted.retain(assetId);
    }

    /**
     * Decrements the reference count for a texture.
     *
     * <p>If the count reaches zero, the texture is deleted from GPU memory and removed
     * from the cache. Called by SceneManager when a scene using this texture is unloaded.
     *
     * @param assetId the engine-assigned texture ID
     */
    public void release(int assetId) {
        refCounted.release(assetId);
    }

    /**
     * Returns the current reference count for a texture.
     *
     * @param assetId the engine-assigned texture ID
     * @return the number of scenes currently using this texture
     */
    public int getRefCount(int assetId) {
        return refCounted.getRefCount(assetId);
    }

    /**
     * Deletes a texture from GPU memory and clears all associated caches.
     *
     * <p>Called by the {@link RefCounted} tracker when a texture's reference count drops to zero.
     * Removes the texture from all internal maps and frees GPU resources via {@link TextureUtil#delete}.
     *
     * @param assetId the engine-assigned texture ID to delete
     */
    private void deleteTexture(int assetId) {
        Texture texture = texturePool.remove(assetId);
        if (texture != null) {
            TextureUtil.delete(texture);
            filePathToIdMap.values().removeIf(id -> id.equals(assetId));
            Log.debug("Texture released and deleted from GPU: id=" + assetId);
        }
    }

    /**
     * Frees ALL textures from GPU memory. Called during engine shutdown.
     *
     * <p>Reference counts are ignored — everything is cleaned up unconditionally.
     */
    @Override
    public void dispose() {
        for (Texture texture : texturePool.values()) {
            TextureUtil.delete(texture);
        }
        texturePool.clear();
        filePathToIdMap.clear();
    }
}
