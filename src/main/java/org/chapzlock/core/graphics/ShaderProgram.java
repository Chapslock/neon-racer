package org.chapzlock.core.graphics;

import java.nio.FloatBuffer;

import org.chapzlock.core.files.FileUtils;
import org.chapzlock.core.math.Matrix4f;
import org.chapzlock.core.math.Vector3f;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL20.*;

public abstract class ShaderProgram implements Shader {
    private final int programId;

    public ShaderProgram(String pathToVertexShaderFile, String pathToFragmentShaderFile) {
        String vertexShaderFile = FileUtils.loadFileAsString(pathToVertexShaderFile);
        String fragmentShaderFile = FileUtils.loadFileAsString(pathToFragmentShaderFile);
        int vertexShaderId = compileShader(vertexShaderFile, GL_VERTEX_SHADER);
        int fragmentShaderId = compileShader(fragmentShaderFile, GL_FRAGMENT_SHADER);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Graphics linking failed: " + glGetProgramInfoLog(programId));
        }

        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }

    private int compileShader(String code, int type) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, code);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Graphics compile failed: " + glGetShaderInfoLog(shaderId));
        }
        return shaderId;
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void onDestroy() {
        unbind();
        glDeleteProgram(programId);
    }

    // Uniform helpers
    public void setUniform(String name, int value) {
        int loc = glGetUniformLocation(programId, name);
        glUniform1i(loc, value);
    }

    public void setUniform(String name, float value) {
        int loc = glGetUniformLocation(programId, name);
        glUniform1f(loc, value);
    }

    public void setUniform(String name, Vector3f value) {
        int location = glGetUniformLocation(programId, name);
        glUniform3f(programId, value.x, value.y, value.z);
    }

    public void setUniform(String name, boolean value) {
        int location = glGetUniformLocation(programId, name);
        float valueToLoad = 0;
        if (value) {
            valueToLoad = 1;
        }
        glUniform1f(location, valueToLoad);
    }

    public void setUniform(String name, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.toBuffer(fb);
            int loc = glGetUniformLocation(programId, name);
            glUniformMatrix4fv(loc, false, fb);
        }
    }
}
