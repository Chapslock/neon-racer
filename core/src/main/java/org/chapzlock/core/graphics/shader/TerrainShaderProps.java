package org.chapzlock.core.graphics.shader;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TerrainShaderProps {
    public static final String VERTEX_FILE = "shaders/TerrainVertex.glsl";
    public static final String FRAGMENT_FILE = "shaders/TerrainFragment.glsl";

    public static final String UNIFORM_TRANSFORMATION_MATRIX = "transformationMatrix";
    public static final String UNIFORM_PROJECTION_MATRIX = "projectionMatrix";
    public static final String UNIFORM_VIEW_MATRIX = "viewMatrix";
    public static final String UNIFORM_LIGHT_POSITION = "lightPosition";
    public static final String UNIFORM_LIGHT_COLOR = "lightColor";
    public static final String UNIFORM_SHINE_DAMPER = "shineDamper";
    public static final String UNIFORM_REFLECTIVITY = "reflectivity";
    public static final String UNIFORM_TEXTURE_SAMPLER = "textureSampler";
}
