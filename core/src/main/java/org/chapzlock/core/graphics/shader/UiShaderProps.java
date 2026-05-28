package org.chapzlock.core.graphics.shader;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UiShaderProps {
    public static final String VERTEX_FILE = "shaders/UiVertex.glsl";
    public static final String FRAGMENT_FILE = "shaders/UiFragment.glsl";

    public static final String UNIFORM_PROJECTION_MATRIX = "projectionMatrix";
    public static final String UNIFORM_TRANSFORMATION_MATRIX = "transformationMatrix";
    public static final String UNIFORM_TEXTURE_SAMPLER = "uTexture";
    public static final String UNIFORM_TINT_COLOR = "uTintColor";
}
