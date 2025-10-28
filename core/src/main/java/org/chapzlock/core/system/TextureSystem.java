package org.chapzlock.core.system;

import java.util.Map;

import org.chapzlock.core.component.Texture;
import org.chapzlock.core.files.FileUtils;
import org.chapzlock.core.graphics.RawImageData;
import org.chapzlock.core.graphics.TextureUtil;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Caches and handles OpenGL calls on textures.
 * Implemented as a singleton to keep track of globally allocated texture resources.
 */
public class TextureSystem {
    private static TextureSystem instance;

    private final Map<Integer, Texture> texturePool = new Int2ObjectOpenHashMap<>();
    private final Map<String, Integer> filePathToIdMap = new Object2IntOpenHashMap<>();

    private TextureSystem() {
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
        RawImageData rawImageData = FileUtils.loadImage(filePath);
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

    public static TextureSystem instance() {
        if (instance == null) {
            instance = new TextureSystem();
        }
        return instance;
    }
}
