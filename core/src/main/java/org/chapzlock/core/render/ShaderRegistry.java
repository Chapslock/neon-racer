package org.chapzlock.core.render;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.Disposable;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.asset.FileUtils;
import org.chapzlock.core.logging.Log;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Compiles, caches, and manages shader programs throughout their lifecycle.
 *
 * <p><strong>What this system does:</strong> Handles the full lifecycle of GPU shader programs:
 * loading GLSL source files, compiling them, linking them into programs, caching uniform
 * locations for fast access, and cleaning up GPU memory when shaders are no longer needed.
 *
 * <p><strong>Why caching matters:</strong>
 * <ul>
 *   <li><em>Program activation:</em> If the same shader is already active, skip the
 *       {@code glUseProgram} call (GPU state changes are expensive)</li>
 *   <li><em>Uniform locations:</em> Querying the GPU for a uniform's location is slow.
 *       We cache the result so subsequent frames just do a HashMap lookup instead.</li>
 *   <li><em>Compilation:</em> A {@link Shader} component stores its vertex/fragment paths.
 *       If it's already been compiled (has a nonzero programId), we skip recompilation.</li>
 * </ul>
 *
 * <p><strong>Reference counting:</strong> Shader programs are shared across scenes. When a
 * scene is loaded, the SceneManager calls {@link #retain(int)} on each program used by that
 * scene's materials. When the scene is unloaded, it calls {@link #release(int)}. A program
 * is only deleted from the GPU when its reference count drops to zero (meaning no loaded
 * scene uses it anymore).
 *
 * <p><strong>Usage:</strong> Game code doesn't interact with this system directly. Render
 * passes and material renderers use it to compile/activate shaders during rendering. The
 * {@link Shader} component on an entity stores which shader files to use.
 *
 * @see Shader
 * @see ShaderUtil
 * @see PbrShaderProps
 */
public class ShaderRegistry implements Disposable {

    private static final Log log = Log.create(ShaderRegistry.class);

    /**
     * Uniform location cache: programId → (uniformName → location).
     * Avoids repeated GPU queries for the same uniform each frame.
     */
    private final Map<Integer, Map<String, Integer>> uniformCache = new HashMap<>();

    /** All program IDs ever created, for cleanup in {@link #dispose()}. */
    private final List<Integer> allProgramIds = new ArrayList<>();

    /** The currently active program — tracked to skip redundant glUseProgram calls. */
    private int currentlyActiveProgramId = 0;

    /**
     * Reference counting tracker for shader program lifecycle.
     *
     * <p>When the reference count for a program ID drops to zero (no scenes use it anymore),
     * the {@link #deleteProgram(int)} callback is fired to clean up GPU resources.
     */
    private final RefCounted refCounted = new RefCounted(this::deleteProgram);

    public ShaderRegistry() {
    }

    /**
     * Activates a shader program for rendering.
     *
     * <p>If the shader hasn't been compiled yet, compiles and links it first.
     * If this program is already active, does nothing (avoids redundant GPU state change).
     *
     * @param shader the shader component (must have vertex and fragment paths set)
     */
    public void use(Shader shader) {
        if (!shader.isCompiled()) {
            compileAndLink(shader);
        }
        if (currentlyActiveProgramId == shader.getProgramId()) {
            return;
        }
        ShaderUtil.useProgram(shader.getProgramId());
        currentlyActiveProgramId = shader.getProgramId();
    }

    /**
     * Compiles vertex and fragment shaders from their source files and links them into a program.
     *
     * <p>If the shader is already compiled (has a nonzero program ID), this method does nothing.
     * After successful compilation, the shader's program ID is set and uniform cache is initialized.
     *
     * <p>The source files are loaded from the classpath using {@link FileUtils#loadAsString(String)}.
     * GLSL files are typically in {@code src/main/resources/shaders/}.
     *
     * @param shader the shader component with vertex/fragment paths to compile
     * @throws RuntimeException if compilation or linking fails (includes GPU error log)
     */
    public void compileAndLink(Shader shader) {
        if (shader.isCompiled()) {
            return;
        }
        long start = System.nanoTime();
        String vertexShader = FileUtils.loadAsString(shader.getVertexPath());
        String fragmentShader = FileUtils.loadAsString(shader.getFragmentPath());
        int vertexShaderId = ShaderUtil.compileShader(vertexShader, GL_VERTEX_SHADER);
        int fragmentShaderId = ShaderUtil.compileShader(fragmentShader, GL_FRAGMENT_SHADER);
        int programId = ShaderUtil.linkShaderProgram(vertexShaderId, fragmentShaderId);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        shader.setProgramId(programId);
        allProgramIds.add(programId);
        uniformCache.put(programId, new HashMap<>());
        log.debug("Shader compiled in %dms: %s + %s → programId=%d", elapsedMs, shader.getVertexPath(), shader.getFragmentPath(), programId);
    }

    /**
     * Deactivates the current shader program.
     *
     * <p>Called between render passes or after rendering is complete to leave a clean state.
     */
    public void clearProgram() {
        ShaderUtil.clearProgram();
        currentlyActiveProgramId = 0;
    }

    /**
     * Deletes a shader program from the GPU and clears its cached data.
     *
     * <p>After this call, the shader's program ID is reset to 0 (uncompiled state).
     * Recompilation will happen automatically next time {@link #use(Shader)} is called.
     *
     * @param shaderComp the shader component to delete
     */
    public void delete(Shader shaderComp) {
        if (!shaderComp.isCompiled()) {
            return;
        }
        ShaderUtil.deleteProgram(shaderComp.getProgramId());
        allProgramIds.remove(Integer.valueOf(shaderComp.getProgramId()));
        uniformCache.remove(shaderComp.getProgramId());
        shaderComp.setProgramId(0);
        currentlyActiveProgramId = 0;
    }

    /**
     * Sets an integer uniform on the given shader.
     *
     * @param shader the shader to set the uniform on (must be currently active)
     * @param name the uniform name as declared in the GLSL source
     * @param value the integer value
     */
    public void setUniform(Shader shader, String name, int value) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), value);
    }

    /**
     * Looks up a uniform's location, using the cache to avoid repeated GPU queries.
     *
     * <p>On first call for a given (program, name) pair, queries the GPU and caches
     * the result. Subsequent calls return the cached value directly.
     *
     * <p>If the uniform doesn't exist in the shader (returns -1), logs a warning on the
     * first occurrence and returns -1. The setUniform calls in {@link ShaderUtil} are
     * no-ops for location -1 (OpenGL ignores uniform operations on invalid locations).
     * This prevents crashes when shaders are edited and unused uniforms are removed.
     *
     * @param shader the shader whose program to query
     * @param name the uniform name as declared in the GLSL source
     * @return the uniform location (>= 0), or -1 if the uniform doesn't exist
     */
    public int getUniformLocation(Shader shader, String name) {
        int programId = shader.getProgramId();
        Map<String, Integer> uniformMappings = uniformCache.computeIfAbsent(programId, k -> new HashMap<>());
        return uniformMappings.computeIfAbsent(name, k -> {
            int uniformLocation = ShaderUtil.getUniformLocation(programId, k);
            if (uniformLocation < 0) {
                log.warn("Uniform \"%s\" not found in shader (vertex=%s, fragment=%s). It may have been removed or optimized away by the GLSL compiler. Check the shader source file.",
                    k, shader.getVertexPath(), shader.getFragmentPath());
            }
            return uniformLocation;
        });
    }

    /** Sets a float uniform. See {@link #setUniform(Shader, String, int)} for details. */
    public void setUniform(Shader shader, String name, float value) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), value);
    }

    /** Sets a vec3 uniform. See {@link #setUniform(Shader, String, int)} for details. */
    public void setUniform(Shader shader, String name, Vector3f vector3f) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), vector3f);
    }

    /** Sets a boolean uniform (as float 0.0/1.0). See {@link #setUniform(Shader, String, int)} for details. */
    public void setUniform(Shader shader, String name, boolean value) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), value);
    }

    /** Sets a mat4 uniform. See {@link #setUniform(Shader, String, int)} for details. */
    public void setUniform(Shader shader, String name, Matrix4f matrix) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), matrix);
    }

    /** Sets a vec4 uniform. See {@link #setUniform(Shader, String, int)} for details. */
    public void setUniform(Shader shader, String name, Vector4f vector4f) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), vector4f);
    }

    /**
     * Increments the reference count for a shader program.
     *
     * <p>Called by SceneManager when a scene is loaded that uses this program.
     *
     * @param programId the program's OpenGL ID
     */
    public void retain(int programId) {
        refCounted.retain(programId);
    }

    /**
     * Decrements the reference count for a shader program.
     *
     * <p>If the count reaches zero, the program is deleted from the GPU. Called by
     * SceneManager when a scene using this program is unloaded.
     *
     * @param programId the program's OpenGL ID
     */
    public void release(int programId) {
        refCounted.release(programId);
    }

    /**
     * Returns the current reference count for a shader program.
     *
     * @param programId the program's OpenGL ID
     * @return the number of scenes currently using this program
     */
    public int getRefCount(int programId) {
        return refCounted.getRefCount(programId);
    }

    /**
     * Deletes a shader program from GPU memory and clears all associated caches.
     *
     * <p>Called by the {@link RefCounted} tracker when a program's reference count drops to zero.
     * Removes the program from all internal maps and frees GPU resources via {@link ShaderUtil#deleteProgram}.
     *
     * @param programId the program's OpenGL ID to delete
     */
    private void deleteProgram(int programId) {
        if (allProgramIds.remove(Integer.valueOf(programId))) {
            ShaderUtil.deleteProgram(programId);
        }
        uniformCache.remove(programId);
        if (currentlyActiveProgramId == programId) {
            currentlyActiveProgramId = 0;
        }
        log.debug("Shader program released and deleted: programId=%d", programId);
    }

    /**
     * Frees ALL shader programs from GPU memory. Called during engine shutdown.
     *
     * <p>After this call, all program IDs are invalid. This is unconditional cleanup —
     * reference counts are ignored because the engine is shutting down.
     */
    @Override
    public void dispose() {
        for (int programId : allProgramIds) {
            ShaderUtil.deleteProgram(programId);
        }
        allProgramIds.clear();
        uniformCache.clear();
        currentlyActiveProgramId = 0;
    }
}
