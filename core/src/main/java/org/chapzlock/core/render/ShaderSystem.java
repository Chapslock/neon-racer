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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Caches shader programs and handles OpenGL calls on shaders.
 */
public class ShaderSystem implements Disposable {

    /**
     * per-program uniform cache: programId -> (name -> location)
     */
    private final Map<Integer, Map<String, Integer>> uniformCache = new HashMap<>();
    private final List<Integer> allProgramIds = new ArrayList<>();
    private int currentlyActiveProgramId = 0;

    public ShaderSystem() {
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
        allProgramIds.add(programId);
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
        allProgramIds.remove(Integer.valueOf(shaderComp.getProgramId()));
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

    public void setUniform(Shader shader, String name, Vector4f vector4f) {
        ShaderUtil.setUniform(getUniformLocation(shader, name), vector4f);
    }

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
