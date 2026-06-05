package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GPU texture handle representing a 2D image uploaded to the graphics card.
 *
 * <p><strong>What is a texture in OpenGL?</strong> A texture is image data stored in GPU memory,
 * identified by an integer ID. The GPU samples (reads) texels (texture pixels) during rendering
 * to color mesh surfaces. Textures are bound to numbered "texture units" (slots) so shaders
 * can reference multiple textures in a single draw call.
 *
 * <p>Instances are created and managed by {@link org.chapzlock.core.render.TextureRegistry},
 * which handles deduplication (same path → same GPU texture) and reference-counted cleanup.
 *
 * @see org.chapzlock.core.render.TextureRegistry
 * @see PbrMaterial
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Texture implements Component {
    private int id;
    private int textureHandle;
    private int width;
    private int height;
    private String filePath;
}
