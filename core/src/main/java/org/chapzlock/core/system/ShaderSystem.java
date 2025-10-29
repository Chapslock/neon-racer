package org.chapzlock.core.system;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

import java.util.HashMap;
import java.util.Map;

import org.chapzlock.core.component.Shader;
import org.chapzlock.core.files.FileUtils;
import org.chapzlock.core.graphics.ShaderUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Caches shader programs and handles OpenGL calls on shaders
 * Implemented as a singleton to keep track of all shader programs.
 */
public class ShaderSystem {
    private static ShaderSystem instance;

    /**
     * per-program uniform cache: programId -> (name -> location)
     */
    private final Map<Integer, Map<String, Integer>> uniformCache = new HashMap<>();
    private static int currentlyActiveProgramId = 0;

    private ShaderSystem() {
    }

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
     * Compiles the shaders from the source files and links them to a shader program.
     *
     * @param shader
     */
    public void compileAndLink(Shader shader) {
        if (shader.isCompiled()) {
            return;
        }
        String vertexShader = FileUtils.loadAsString(shader.getVertexPath());
        String fragmentShader = FileUtils.loadAsString(shader.getFragmentPath());
        int vertexShaderId = ShaderUtil.compileShader(vertexShader, GL_VERTEX_SHADER);
        int fragmentShaderId = ShaderUtil.compileShader(fragmentShader, GL_FRAGMENT_SHADER);
        int programId = ShaderUtil.linkShaderProgram(vertexShaderId, fragmentShaderId);
        shader.setProgramId(programId);
        uniformCache.put(programId, new HashMap<>());
    }

    public void clearProgram() {
        ShaderUtil.clearProgram();
        currentlyActiveProgramId = 0;
    }

    public void delete(Shader shaderComp) {
        if (!shaderComp.isCompiled()) {
            return;
        }
        ShaderUtil.deleteProgram(shaderComp.getProgramId());
        uniformCache.remove(shaderComp.getProgramId());
        shaderComp.setProgramId(0);
        currentlyActiveProgramId = 0;
    }

    public void setUniform(Shader shader, String name, int value) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), value);
    }

    /**
     * Finds a uniform variable location and caches it
     *
     * @param shader
     * @param name   name of the unifrom variable
     * @return location of the uniform variable
     */
    public int getUniformLocation(Shader shader, String name) {
        int programId = shader.getProgramId();
        Map<String, Integer> uniformMappings = uniformCache.computeIfAbsent(programId, k -> new HashMap<>());
        return uniformMappings.computeIfAbsent(name, k -> {
            int uniformLocation = ShaderUtil.getUniformLocation(programId, k);
            if (uniformLocation < 0) {
                throw new RuntimeException("Uniform not found: " + k);
            }
            return uniformLocation;
        });
    }

    public void setUniform(Shader shader, String name, float value) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), value);
    }

    public void setUniform(Shader shader, String name, Vector3f vector3f) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), vector3f);
    }

    public void setUniform(Shader shader, String name, boolean value) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), value);
    }

    public void setUniform(Shader shader, String name, Matrix4f matrix) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), matrix);
    }

    public static ShaderSystem instance() {
        if (instance == null) {
            instance = new ShaderSystem();
        }
        return instance;
    }
}
