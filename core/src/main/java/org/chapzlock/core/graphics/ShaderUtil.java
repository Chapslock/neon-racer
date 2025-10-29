package org.chapzlock.core.graphics;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
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

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import lombok.experimental.UtilityClass;

/**
 * Handles shader related OpenGL calls
 */
@UtilityClass
public class ShaderUtil {

    /**
     * Compiles raw shader code on the GPU
     *
     * @param shaderCode
     * @param shaderType vertex or fragment shader type
     * @return OpenGL handle to the shader
     */
    public static int compileShader(String shaderCode, int shaderType) {
        int id = glCreateShader(shaderType);
        glShaderSource(id, shaderCode);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(id);
            glDeleteShader(id);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return id;
    }

    /**
     * Links the vertex and fragment shader to a shader program
     *
     * @param vertexShaderId
     * @param fragmentShaderId
     * @return OpenGL handle for the shader program
     */
    public static int linkShaderProgram(int vertexShaderId, int fragmentShaderId) {
        int programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            glDeleteProgram(programId);
            glDeleteShader(vertexShaderId);
            glDeleteShader(fragmentShaderId);
            throw new RuntimeException("Shader link failed: " + log);
        }
        // shaders can be deleted after linking
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
        return programId;
    }

    /**
     * Installs a shader program as part of current rendering context
     *
     * @param programId
     */
    public static void useProgram(int programId) {
        glUseProgram(programId);
    }

    /**
     * Clears the currently active shader program
     */
    public static void clearProgram() {
        glUseProgram(0);
    }

    /**
     * Deletes an OpenGL shader program
     *
     * @param programId
     */
    public static void deleteProgram(int programId) {
        glDeleteProgram(programId);
    }

    /**
     * Returns the location of the uniform variable
     *
     * @param programId the program object to be queried
     * @param name      of the unifrom variable to query
     * @return location of the uniform vaiable
     */
    public static int getUniformLocation(int programId, String name) {
        return glGetUniformLocation(programId, name);
    }

    /**
     * Sets the value of a uniform variable
     *
     * @param uniformLocation
     * @param value
     */
    public static void setUniform(int uniformLocation, int value) {
        glUniform1i(uniformLocation, value);
    }

    /**
     * Sets the value of a uniform variable
     *
     * @param uniformLocation
     * @param value
     */
    public static void setUniform(int uniformLocation, float value) {
        glUniform1f(uniformLocation, value);
    }


    /**
     * Sets the value of a uniform variable
     *
     * @param uniformLocation
     * @param value
     */
    public static void setUniform(int uniformLocation, Vector3f value) {
        glUniform3f(uniformLocation, value.x, value.y, value.z);
    }

    /**
     * Sets the value of a uniform variable
     *
     * @param uniformLocation
     * @param value
     */
    public static void setUniform(int uniformLocation, boolean value) {
        glUniform1f(uniformLocation, value ? 1f : 0f);
    }

    /**
     * Sets the value of a uniform variable
     *
     * @param uniformLocation
     * @param value
     */
    public static void setUniform(int uniformLocation, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            glUniformMatrix4fv(uniformLocation, false, fb);
        }
    }
}
