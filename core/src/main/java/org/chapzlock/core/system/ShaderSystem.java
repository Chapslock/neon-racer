package org.chapzlock.core.system;

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
import java.util.HashMap;
import java.util.Map;

import org.chapzlock.core.component.Shader;
import org.chapzlock.core.files.FileUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class ShaderSystem {

    /**
     * per-program uniform cache: programId -> (name -> location)
     *
     */
    private final Map<Integer, Map<String, Integer>> uniformCache = new HashMap<>();

    public void use(Shader shaderComp) {
        if (!shaderComp.isCompiled()) {
            compileAndLink(shaderComp);
        }
        glUseProgram(shaderComp.getProgramId());
    }

    public void compileAndLink(Shader shaderComp) {
        if (shaderComp.isCompiled()) {
            return;
        }

        String vertexShader = FileUtils.loadAsString(shaderComp.getVertexPath());
        String fragmentShader = FileUtils.loadAsString(shaderComp.getFragmentPath());

        int vertexShaderId = compileShader(vertexShader, GL_VERTEX_SHADER);
        int fragmentShaderId = compileShader(fragmentShader, GL_FRAGMENT_SHADER);

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

        shaderComp.setProgramId(programId);
        uniformCache.put(programId, new HashMap<>());
    }

    private int compileShader(String src, int type) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(id);
            glDeleteShader(id);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return id;
    }

    public void stop() {
        glUseProgram(0);
    }

    public void delete(Shader shaderComp) {
        int programId = shaderComp.getProgramId();
        if (programId != 0) {
            glDeleteProgram(programId);
            uniformCache.remove(programId);
            shaderComp.setProgramId(0);
        }
    }

    public void setUniform(Shader shaderComp, String name, int value) {
        glUniform1i(getUniformLocation(shaderComp, name), value);
    }

    /* uniform helpers */
    public int getUniformLocation(Shader shaderComp, String name) {
        int programId = shaderComp.getProgramId();
        Map<String, Integer> map = uniformCache.computeIfAbsent(programId, k -> new HashMap<>());
        return map.computeIfAbsent(name, k -> {
            int loc = glGetUniformLocation(programId, k);
            if (loc < 0) {
                throw new RuntimeException("Uniform not found: " + k);
            }
            return loc;
        });
    }

    public void setUniform(Shader shaderComp, String name, float value) {
        glUniform1f(getUniformLocation(shaderComp, name), value);
    }

    public void setUniform(Shader shaderComp, String name, Vector3f vec) {
        glUniform3f(getUniformLocation(shaderComp, name), vec.x, vec.y, vec.z);
    }

    public void setUniform(Shader shaderComp, String name, boolean value) {
        glUniform1f(getUniformLocation(shaderComp, name), value ? 1f : 0f);
    }

    public void setUniform(Shader shaderComp, String name, Matrix4f mat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            mat.get(fb);
            glUniformMatrix4fv(getUniformLocation(shaderComp, name), false, fb);
        }
    }
}
