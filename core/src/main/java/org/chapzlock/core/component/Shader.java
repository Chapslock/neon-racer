package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Shader component storing GLSL source paths and compiled program ID.
 *
 * <p>Created by specifying vertex and fragment shader paths.
 * Compiled by {@link org.chapzlock.core.render.ShaderRegistry}.
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
