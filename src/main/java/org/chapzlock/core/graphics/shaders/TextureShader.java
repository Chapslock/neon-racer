package org.chapzlock.core.graphics.shaders;

import org.chapzlock.core.component.LightComponent;
import org.chapzlock.core.graphics.ShaderProgram;
import org.joml.Matrix4f;

public class TextureShader extends ShaderProgram {

    private static final String VERTEX_FILE = "shaders/textured.vertex.glsl";
    private static final String FRAGMENT_FILE = "shaders/textured.fragment.glsl";

    private static final String UNIFORM_TRANSFORMATION_MATRIX = "transformationMatrix";
    private static final String UNIFORM_PROJECTION_MATRIX = "projectionMatrix";
    private static final String UNIFORM_VIEW_MATRIX = "viewMatrix";
    private static final String UNIFORM_LIGHT_POSITION = "lightPosition";
    private static final String UNIFORM_LIGHT_COLOR = "lightColor";

    public TextureShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }

    public void loadTransformationMatrix(Matrix4f matrix) {
        setUniform(UNIFORM_TRANSFORMATION_MATRIX, matrix);
    }

    public void loadProjectionMatrix(Matrix4f matrix) {
        setUniform(UNIFORM_PROJECTION_MATRIX, matrix);
    }

    public void loadViewMatrix(Matrix4f matrix4f) {
        setUniform(UNIFORM_VIEW_MATRIX, matrix4f);
    }

    public void loadLight(LightComponent light) {
        setUniform(UNIFORM_LIGHT_POSITION, light.position());
        setUniform(UNIFORM_LIGHT_COLOR, light.color().toVector3f());
    }
}
