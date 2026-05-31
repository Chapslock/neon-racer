package org.chapzlock.core.render;

import java.util.Map;

import org.chapzlock.core.application.Disposable;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.asset.FileUtils;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Loads, caches, and manages GPU texture resources.
 *
 * <p>Textures are reference-counted — use {@link #retain(org.chapzlock.core.component.Texture)} and
 * {@link #release(org.chapzlock.core.component.Texture)} to manage lifetimes across scenes.
 */
public class TextureSystem implements Disposable {

    private final Map<Integer, Texture> texturePool = new Int2ObjectOpenHashMap<>();
    private final Map<String, Integer> filePathToIdMap = new Object2IntOpenHashMap<>();
    private final Int2IntOpenHashMap refCounts = new Int2IntOpenHashMap();

    public TextureSystem() {
    }

    /**
     * Load a texture from a file and creates a Texture (calls GL).
     * @param filePath
     * @return texture bound to GPU
     */
    public Texture load(String filePath) {
        Integer existingId = filePathToIdMap.get(filePath);
        if (existingId != null) {
            return texturePool.get(existingId);
        }
        RawImageData rawImageData = FileUtils.loadRawImage(filePath);
        Texture texture = TextureUtil.bindTextureDataToGpu(rawImageData);
        texturePool.put(texture.getId(), texture);
        filePathToIdMap.put(texture.getFilePath(), texture.getId());
        return texture;
    }

    /**
     * Loads a texture from in-memory image bytes (PNG/JPEG) and uploads to GPU.
     *
     * <p>Used for embedded GLB textures where the image data lives in the binary
     * chunk rather than as a separate file. Caches by cacheKey to avoid redundant
     * decoding and GPU uploads when the same embedded texture is referenced multiple times.
     *
     * @param imageBytes raw PNG/JPEG bytes from the GLB binary chunk
     * @param cacheKey   unique identifier for caching (e.g., "embedded:model.glb:tex0")
     * @return GPU-bound texture ready for rendering
     */
    public Texture loadFromMemory(java.nio.ByteBuffer imageBytes, String cacheKey) {
        Integer existingId = filePathToIdMap.get(cacheKey);
        if (existingId != null) {
            return texturePool.get(existingId);
        }
        RawImageData rawImageData = FileUtils.loadRawImageFromMemory(imageBytes, cacheKey);
        Texture texture = TextureUtil.bindTextureDataToGpu(rawImageData);
        texturePool.put(texture.getId(), texture);
        filePathToIdMap.put(cacheKey, texture.getId());
        return texture;
    }

    /**
     * Binds a texture for rendering
     *
     * @param texture
     * @param unit
     */
    public void bind(Texture texture, int unit) {
        if (texture == null) {
            return;
        }
        TextureUtil.bind(texture, unit);
    }

    /**
     * Unbinds a texture after rendering
     *
     * @param texture
     */
    public void unbind(Texture texture) {
        if (texture == null) {
            return;
        }
        TextureUtil.unbind();
    }

    public void delete(Texture texture) {
        if (texture == null) {
            return;
        }
        TextureUtil.delete(texture);
        texturePool.remove(texture.getId());
    }

    public void retain(int assetId) {
        refCounts.put(assetId, refCounts.getOrDefault(assetId, 0) + 1);
    }

    public void release(int assetId) {
        int count = refCounts.getOrDefault(assetId, 0) - 1;
        if (count <= 0) {
            refCounts.remove(assetId);
            Texture texture = texturePool.remove(assetId);
            if (texture != null) {
                TextureUtil.delete(texture);
                filePathToIdMap.values().removeIf(id -> id.equals(assetId));
            }
        } else {
            refCounts.put(assetId, count);
        }
    }

    public int getRefCount(int assetId) {
        return refCounts.getOrDefault(assetId, 0);
    }

    @Override
    public void dispose() {
        for (Texture texture : texturePool.values()) {
            TextureUtil.delete(texture);
        }
        texturePool.clear();
        filePathToIdMap.clear();
    }
}
