package org.chapzlock.core.asset.gltf;

import java.nio.ByteBuffer;

/**
 * Reference to a texture that is either file-based (external URI) or memory-based
 * (embedded in a GLB binary chunk).
 *
 * <p><strong>Why two modes?</strong> glTF files can reference textures in two ways:
 * <ul>
 *   <li><em>External:</em> The texture is a separate image file on disk (has a URI)</li>
 *   <li><em>Embedded:</em> The texture bytes are packed inside the GLB binary chunk (no URI)</li>
 * </ul>
 *
 * <p>The engine's TextureSystem needs different loading paths for each case:
 * external textures are loaded from the filesystem via STB, while embedded textures
 * are decoded from an in-memory buffer.
 *
 * @param filePath   file path for external textures, null for embedded
 * @param imageBytes raw image bytes (PNG/JPEG) for embedded textures, null for external
 * @param cacheKey   unique key for TextureSystem caching (file path for external,
 *                   synthetic key like "embedded:model.glb:tex0" for embedded)
 */
public record GltfTextureRef(
    String filePath,
    ByteBuffer imageBytes,
    String cacheKey
) {
    public boolean isEmbedded() {
        return imageBytes != null;
    }

    public static GltfTextureRef fromPath(String path) {
        return new GltfTextureRef(path, null, path);
    }

    public static GltfTextureRef fromMemory(ByteBuffer bytes, String cacheKey) {
        return new GltfTextureRef(null, bytes, cacheKey);
    }
}
