package org.chapzlock.core.render;

/**
 * Shader file paths and uniform names for the PBR material renderer.
 *
 * <p>This is a constants-only class that centralizes all shader-related string values
 * so they're defined in exactly one place. The uniform names here must match the
 * uniform declarations in PbrVertex.glsl and PbrFragment.glsl.
 *
 * <p><strong>Why a constants class?</strong> Typos in uniform names are a common source of
 * silent bugs in OpenGL (setting a misspelled uniform does nothing, no error). Having them
 * as named constants enables compile-time safety and IDE autocomplete.
 *
 * @see PbrMaterialRenderer
 */
public final class PbrShaderProps {
    public static final String VERTEX_FILE = "shaders/PbrVertex.glsl";
    public static final String FRAGMENT_FILE = "shaders/PbrFragment.glsl";

    // Matrices
    public static final String UNIFORM_MODEL_MATRIX = "uModelMatrix";
    public static final String UNIFORM_VIEW_MATRIX = "uViewMatrix";
    public static final String UNIFORM_PROJECTION_MATRIX = "uProjectionMatrix";

    // Material factors
    public static final String UNIFORM_BASE_COLOR_FACTOR = "uBaseColorFactor";
    public static final String UNIFORM_METALLIC_FACTOR = "uMetallicFactor";
    public static final String UNIFORM_ROUGHNESS_FACTOR = "uRoughnessFactor";
    public static final String UNIFORM_EMISSIVE_FACTOR = "uEmissiveFactor";
    public static final String UNIFORM_ALPHA_CUTOFF = "uAlphaCutoff";
    public static final String UNIFORM_ALPHA_MODE = "uAlphaMode";

    // Texture samplers
    public static final String UNIFORM_BASE_COLOR_MAP = "uBaseColorMap";
    public static final String UNIFORM_METALLIC_ROUGHNESS_MAP = "uMetallicRoughnessMap";
    public static final String UNIFORM_NORMAL_MAP = "uNormalMap";
    public static final String UNIFORM_OCCLUSION_MAP = "uOcclusionMap";
    public static final String UNIFORM_EMISSIVE_MAP = "uEmissiveMap";

    // Feature flags
    public static final String UNIFORM_HAS_BASE_COLOR_MAP = "uHasBaseColorMap";
    public static final String UNIFORM_HAS_METALLIC_ROUGHNESS_MAP = "uHasMetallicRoughnessMap";
    public static final String UNIFORM_HAS_NORMAL_MAP = "uHasNormalMap";
    public static final String UNIFORM_HAS_OCCLUSION_MAP = "uHasOcclusionMap";
    public static final String UNIFORM_HAS_EMISSIVE_MAP = "uHasEmissiveMap";

    // Skeletal animation (GPU skinning)
    /** Uniform flag indicating the entity has a skeleton and should apply skinning. */
    public static final String UNIFORM_HAS_SKIN = "uHasSkin";
    /** Uniform array of joint transformation matrices (world * inverseBindMatrix per joint). */
    public static final String UNIFORM_JOINT_MATRICES = "uJointMatrices";
    /** Maximum number of joints supported by the shader. Must match MAX_JOINTS in PbrVertex.glsl. */
    public static final int MAX_JOINTS = 128;

    // Lighting
    public static final String UNIFORM_CAMERA_POSITION = "uCameraPosition";
    public static final String UNIFORM_AMBIENT_STRENGTH = "uAmbientStrength";
    public static final String UNIFORM_HAS_DIR_LIGHT = "uHasDirLight";
    public static final String UNIFORM_DIR_LIGHT_DIRECTION = "uDirLightDirection";
    public static final String UNIFORM_DIR_LIGHT_COLOR = "uDirLightColor";
    public static final String UNIFORM_NUM_POINT_LIGHTS = "uNumPointLights";
    public static final String UNIFORM_POINT_LIGHT_POSITIONS = "uPointLightPositions";
    public static final String UNIFORM_POINT_LIGHT_COLORS = "uPointLightColors";
    public static final String UNIFORM_POINT_LIGHT_ATTENUATION = "uPointLightAttenuation";

    private PbrShaderProps() {}
}
