package org.chapzlock.core.graphics;

import java.nio.ByteBuffer;

import org.lwjgl.stb.STBImage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * POJO for a raw image loaded from an image file
 */
@Getter
@RequiredArgsConstructor
public class RawImageData {
    private final ByteBuffer imageData;
    private final int width;
    private final int height;
    private final String filePath;

    /**
     * Frees the resources allocated by the imageData ByteBuffer.
     * Usually called after the image data has been uploaded to the GPU
     */
    public void deleteBufferFromMemory() {
        STBImage.stbi_image_free(imageData);
    }
}
