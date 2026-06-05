package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * GPU shader program handle referencing compiled GLSL code.
 *
 * <p><strong>What is a shader?</strong> A shader is a small program that runs on the GPU.
 * A shader program consists of at least two stages:
 * <ul>
 *   <li><strong>Vertex shader</strong> — runs once per vertex, transforms 3D positions to screen space</li>
 *   <li><strong>Fragment shader</strong> — runs once per pixel, computes the final color</li>
 * </ul>
 *
 * <p>Shaders are written in GLSL (OpenGL Shading Language) and compiled at runtime.
 * The {@link org.chapzlock.core.render.ShaderRegistry} handles compilation, caching, and
 * uniform location lookups.
 *
 * @see org.chapzlock.core.render.ShaderRegistry
 */
@Builder
@Getter
@AllArgsConstructor
public class Shader implements Component {
    private final String vertexPath;
    private final String fragmentPath;
    @Setter
    private int programId;

    public Shader(String vertexPath, String fragmentPath) {
        this.vertexPath = vertexPath;
        this.fragmentPath = fragmentPath;
        this.programId = 0;
    }

    /** Returns true if the shader has been compiled (programId != 0). */
    public boolean isCompiled() {
        return programId != 0;
    }
}
