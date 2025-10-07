package org.chapzlock.core.graphics.shaders;

import org.chapzlock.core.graphics.ShaderProgram;
import org.chapzlock.core.math.Matrix4f;

public class StaticShader extends ShaderProgram {

    private static final String VERTEX_FILE = "shaders/debug.vert.glsl";
    private static final String FRAGMENT_FILE = "shaders/debug.frag.glsl";

    private static final String UNIFORM_TRANSFORMATION_MATRIX = "transformationMatrix";
    private static final String UNIFORM_PROJECTION_MATRIX = "projectionMatrix";
    private static final String UNIFORM_VIEW_MATRIX = "viewMatrix";

    public StaticShader() {
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
}
