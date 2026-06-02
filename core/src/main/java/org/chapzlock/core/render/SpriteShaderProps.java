package org.chapzlock.core.render;

import lombok.experimental.UtilityClass;

/**
 * Shader file paths and uniform names for the sprite rendering shader.
 *
 * <p>These constants are used by {@link SpriteRenderer} to locate shader files
 * and set uniform values. The sprite shader renders textured quads in screen-space
 * with orthographic projection.
 */
@UtilityClass
public class SpriteShaderProps {
    public static final String VERTEX_FILE = "shaders/UiVertex.glsl";
    public static final String FRAGMENT_FILE = "shaders/UiFragment.glsl";

    public static final String UNIFORM_PROJECTION_MATRIX = "projectionMatrix";
    public static final String UNIFORM_TRANSFORMATION_MATRIX = "transformationMatrix";
    public static final String UNIFORM_TEXTURE_SAMPLER = "uTexture";
    public static final String UNIFORM_TINT_COLOR = "uTintColor";
}
