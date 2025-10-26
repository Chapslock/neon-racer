package org.chapzlock.core.graphics.shader;

import org.chapzlock.core.component.Component;
import org.chapzlock.core.graphics.PointLight;
import org.chapzlock.core.graphics.Shader;
import org.joml.Matrix4f;

public final class TerrainStaticShader extends Shader implements Component {

    private static final String VERTEX_FILE = "shaders/TerrainVertex.glsl";
    private static final String FRAGMENT_FILE = "shaders/TerrainFragment.glsl";

    private static final String UNIFORM_TRANSFORMATION_MATRIX = "transformationMatrix";
    private static final String UNIFORM_PROJECTION_MATRIX = "projectionMatrix";
    private static final String UNIFORM_VIEW_MATRIX = "viewMatrix";
    private static final String UNIFORM_LIGHT_POSITION = "lightPosition";
    private static final String UNIFORM_LIGHT_COLOR = "lightColor";
    private static final String UNIFORM_SHINE_DAMPER = "shineDamper";   // fixed typo
    private static final String UNIFORM_REFLECTIVITY = "reflectivity";
    private static final String UNIFORM_TEXTURE_SAMPLER = "textureSampler";

    public TerrainStaticShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
        createUniform(UNIFORM_TRANSFORMATION_MATRIX);
        createUniform(UNIFORM_PROJECTION_MATRIX);
        createUniform(UNIFORM_VIEW_MATRIX);
        createUniform(UNIFORM_LIGHT_POSITION);
        createUniform(UNIFORM_LIGHT_COLOR);
        createUniform(UNIFORM_SHINE_DAMPER);
        createUniform(UNIFORM_REFLECTIVITY);
        createUniform(UNIFORM_TEXTURE_SAMPLER);
    }

    public void loadTransformationMatrix(Matrix4f matrix) {
        setUniform(UNIFORM_TRANSFORMATION_MATRIX, matrix);
    }

    public void loadProjectionMatrix(Matrix4f matrix) {
        setUniform(UNIFORM_PROJECTION_MATRIX, matrix);
    }

    public void loadViewMatrix(Matrix4f matrix) {
        setUniform(UNIFORM_VIEW_MATRIX, matrix);
    }

    public void loadLight(PointLight light) {
        setUniform(UNIFORM_LIGHT_POSITION, light.position());
        setUniform(UNIFORM_LIGHT_COLOR, light.color().toVector3f());
    }

    public void loadShine(float shineDamper, float reflectivity) {
        setUniform(UNIFORM_SHINE_DAMPER, shineDamper);
        setUniform(UNIFORM_REFLECTIVITY, reflectivity);
    }

    public void loadTexture() {
        setUniform(UNIFORM_TEXTURE_SAMPLER, 0); // tells shader to use texture unit 0
    }
}
