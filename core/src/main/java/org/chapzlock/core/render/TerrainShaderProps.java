package org.chapzlock.core.render;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TerrainShaderProps {

    public static final String VERTEX_FILE = "shaders/TerrainVertex.glsl";
    public static final String FRAGMENT_FILE = "shaders/TerrainFragment.glsl";

    public static final String UNIFORM_TRANSFORMATION_MATRIX = "transformationMatrix";
    public static final String UNIFORM_PROJECTION_MATRIX = "projectionMatrix";
    public static final String UNIFORM_VIEW_MATRIX = "viewMatrix";
    public static final String UNIFORM_CAMERA_POSITION = "cameraPosition";
    public static final String UNIFORM_SHINE_DAMPER = "shineDamper";
    public static final String UNIFORM_REFLECTIVITY = "reflectivity";
    public static final String UNIFORM_TEXTURE_0 = "backgroundTexture";
    public static final String UNIFORM_TEXTURE_1 = "rTexture";
    public static final String UNIFORM_TEXTURE_2 = "bTexture";
    public static final String UNIFORM_TEXTURE_BLEND_MAP = "blendMapTexture";
    public static final String UNIFORM_SKY_COLOR = "skyColor";
    public static final String UNIFORM_AMBIENT_STRENGTH = "ambientStrength";
    public static final String UNIFORM_FOG_DENSITY = "fogDensity";
    public static final String UNIFORM_FOG_GRADIENT = "fogGradient";
    public static final String UNIFORM_FOG_ENABLED = "fogEnabled";
    public static final String UNIFORM_HAS_DIR_LIGHT = "hasDirLight";
    public static final String UNIFORM_DIR_LIGHT_DIRECTION = "dirLightDirection";
    public static final String UNIFORM_DIR_LIGHT_COLOR = "dirLightColor";
    public static final String UNIFORM_NUM_POINT_LIGHTS = "numPointLights";
    public static final String UNIFORM_POINT_LIGHT_POSITIONS = "pointLightPositions";
    public static final String UNIFORM_POINT_LIGHT_COLORS = "pointLightColors";
    public static final String UNIFORM_POINT_LIGHT_ATTENUATION = "pointLightAttenuation";
}
