package org.chapzlock.core.render;

import java.util.Map;

import org.chapzlock.core.application.Disposable;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.asset.FileUtils;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Caches and handles OpenGL calls on textures.
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
