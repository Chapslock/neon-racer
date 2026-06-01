package org.chapzlock.core.render;

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
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import lombok.experimental.UtilityClass;

/**
 * Low-level OpenGL calls for shader compilation, linking, and uniform uploads.
 *
 * <p><strong>What is a shader?</strong> A shader is a small program that runs on the GPU.
 * The vertex shader positions each triangle corner on screen; the fragment shader determines
 * the color of each pixel inside those triangles. Together they define how a 3D model appears.
 *
 * <p><strong>Compilation pipeline:</strong>
 * <ol>
 *   <li><em>Compile</em> — GPU compiles the GLSL source code into GPU machine code.
 *       This happens separately for the vertex and fragment shaders.</li>
 *   <li><em>Link</em> — GPU links the compiled vertex and fragment shaders into a single
 *       "program" that can be activated for rendering. After linking, the individual shader
 *       objects are deleted (their code lives in the linked program).</li>
 *   <li><em>Use</em> — Activating a program makes it the current rendering pipeline. All
 *       subsequent draw calls use this program until another is activated.</li>
 * </ol>
 *
 * <p><strong>What are uniforms?</strong> Uniforms are global variables you send from Java to the
 * shader each frame. Examples: the camera's projection matrix, a light's color, a texture unit
 * index. They're called "uniform" because every vertex/pixel in a single draw call sees the same
 * value (unlike vertex attributes which vary per-vertex).
 *
 * <p><strong>Uniform locations:</strong> Each uniform has an integer "location" that OpenGL
 * assigns during linking. You must query this location by name before you can set the value.
 * Locations are stable for the lifetime of a program (they don't change frame-to-frame), so
 * they should be cached.
 *
 * @see ShaderSystem
 * @see PbrShaderProps
 */
@UtilityClass
public class ShaderUtil {

    /**
     * Compiles GLSL source code into a GPU shader object.
     *
     * <p>The GPU parses and compiles the shader source into its internal representation.
     * If the source has syntax errors, compilation fails and this method throws with the
     * GPU's error log (which includes line numbers referencing the GLSL source).
     *
     * @param shaderCode the GLSL source code as a string
     * @param shaderType the shader stage — use {@code GL_VERTEX_SHADER} or {@code GL_FRAGMENT_SHADER}
     * @return an OpenGL shader object ID (used only during linking, then deleted)
     * @throws RuntimeException if compilation fails, with the GPU's error log
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
     * Links a compiled vertex shader and fragment shader into a GPU program.
     *
     * <p>Linking resolves connections between the vertex shader's outputs and the fragment
     * shader's inputs (e.g., the vertex shader outputs a UV coordinate, the fragment shader
     * reads it). If the outputs/inputs don't match, linking fails.
     *
     * <p>After successful linking, the individual shader objects are deleted — their code
     * now lives inside the linked program and the objects are no longer needed.
     *
     * @param vertexShaderId the compiled vertex shader object ID
     * @param fragmentShaderId the compiled fragment shader object ID
     * @return an OpenGL program ID — use this with {@link #useProgram} to activate rendering
     * @throws RuntimeException if linking fails, with the GPU's error log
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
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
        return programId;
    }

    /**
     * Activates a shader program as the current rendering pipeline.
     *
     * <p>All subsequent draw calls will use this program's vertex and fragment shaders
     * until another program is activated or {@link #clearProgram()} is called.
     *
     * @param programId the linked program to activate
     */
    public static void useProgram(int programId) {
        glUseProgram(programId);
    }

    /**
     * Deactivates the current shader program.
     *
     * <p>After this call, no shader program is active. Any draw calls would use OpenGL's
     * fixed-function pipeline (which we don't use — this is just cleanup between passes).
     */
    public static void clearProgram() {
        glUseProgram(0);
    }

    /**
     * Deletes a shader program from GPU memory.
     *
     * <p>Call this when a shader is no longer needed (scene unload, reference count reaches
     * zero). The program ID becomes invalid after this call.
     *
     * @param programId the program to delete
     */
    public static void deleteProgram(int programId) {
        glDeleteProgram(programId);
    }

    /**
     * Queries the GPU for a uniform variable's location within a shader program.
     *
     * <p>Uniform locations are assigned by the GPU during linking and remain stable for the
     * program's lifetime. Returns -1 if the uniform doesn't exist in the program (either
     * it was never declared, or the compiler optimized it away because it's unused in the
     * GLSL code).
     *
     * @param programId the linked program to query
     * @param name the exact name of the uniform as declared in the GLSL source
     * @return the uniform location (>= 0), or -1 if not found
     */
    public static int getUniformLocation(int programId, String name) {
        return glGetUniformLocation(programId, name);
    }

    /**
     * Uploads an integer value to a uniform variable.
     *
     * <p>Common uses: setting a texture sampler to its texture unit index (e.g., 0 for
     * GL_TEXTURE0), passing enum-like flags, or setting boolean uniforms as 0/1.
     *
     * @param uniformLocation the location obtained from {@link #getUniformLocation}
     * @param value the integer value to upload
     */
    public static void setUniform(int uniformLocation, int value) {
        glUniform1i(uniformLocation, value);
    }

    /**
     * Uploads a float value to a uniform variable.
     *
     * <p>Common uses: metallic factor, roughness, alpha cutoff, time.
     *
     * @param uniformLocation the location obtained from {@link #getUniformLocation}
     * @param value the float value to upload
     */
    public static void setUniform(int uniformLocation, float value) {
        glUniform1f(uniformLocation, value);
    }

    /**
     * Uploads a 3-component vector to a uniform variable.
     *
     * <p>Common uses: positions (camera, light), directions (light direction),
     * colors (RGB without alpha), scale factors.
     *
     * @param uniformLocation the location obtained from {@link #getUniformLocation}
     * @param value the vec3 value to upload
     */
    public static void setUniform(int uniformLocation, Vector3f value) {
        glUniform3f(uniformLocation, value.x, value.y, value.z);
    }

    /**
     * Uploads a boolean value to a uniform variable (as a float: 1.0 = true, 0.0 = false).
     *
     * <p>GLSL doesn't have a native boolean uniform type in older versions, so we pass
     * booleans as floats and test {@code > 0.5} in the shader.
     *
     * @param uniformLocation the location obtained from {@link #getUniformLocation}
     * @param value true or false
     */
    public static void setUniform(int uniformLocation, boolean value) {
        glUniform1f(uniformLocation, value ? 1f : 0f);
    }

    /**
     * Uploads a 4x4 matrix to a uniform variable.
     *
     * <p>Common uses: model matrix (positions an entity in the world), view matrix (positions
     * the camera), projection matrix (applies perspective distortion), joint/bone matrices
     * for skeletal animation.
     *
     * <p>Uses stack-allocated memory for the temporary float buffer to avoid heap allocation.
     *
     * @param uniformLocation the location obtained from {@link #getUniformLocation}
     * @param value the 4x4 matrix to upload (column-major, matching OpenGL convention)
     */
    public static void setUniform(int uniformLocation, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            glUniformMatrix4fv(uniformLocation, false, fb);
        }
    }

    /**
     * Uploads a 4-component vector to a uniform variable.
     *
     * <p>Common uses: RGBA colors (with alpha), base color factors, quaternions passed as vec4.
     *
     * @param uniformLocation the location obtained from {@link #getUniformLocation}
     * @param vector4f the vec4 value to upload
     */
    public static void setUniform(int uniformLocation, Vector4f vector4f) {
        glUniform4f(uniformLocation, vector4f.x, vector4f.y, vector4f.z, vector4f.w);
    }
}
