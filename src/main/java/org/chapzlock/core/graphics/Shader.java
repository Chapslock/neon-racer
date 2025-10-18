package org.chapzlock.core.graphics;

import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FALSE;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.nio.FloatBuffer;

import org.chapzlock.core.files.FileUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

/**
 * Shader program that directly links to an OpenGL shader program
 */
public abstract class Shader {
    private final int programId;

    protected Shader(String pathToVertexShaderFile, String pathToFragmentShaderFile) {
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

    public void onDestroy() {
        unbind();
        glDeleteProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    protected void setUniform(String name, int value) {
        int loc = glGetUniformLocation(programId, name);
        glUniform1i(loc, value);
    }

    protected void setUniform(String name, float value) {
        int loc = glGetUniformLocation(programId, name);
        glUniform1f(loc, value);
    }

    protected void setUniform(String name, Vector3f value) {
        int location = glGetUniformLocation(programId, name);
        glUniform3f(location, value.x, value.y, value.z);
    }

    protected void setUniform(String name, boolean value) {
        int location = glGetUniformLocation(programId, name);
        float valueToLoad = 0;
        if (value) {
            valueToLoad = 1;
        }
        glUniform1f(location, valueToLoad);
    }

    protected void setUniform(String name, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            int loc = glGetUniformLocation(programId, name);
            glUniformMatrix4fv(loc, false, fb);
        }
    }
}
