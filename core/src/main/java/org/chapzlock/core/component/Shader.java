package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Data-only shader component: stores source paths and program id once compiled.
 *
 */
@Builder
@Getter
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

    public boolean isCompiled() {
        return programId != 0;
    }
}
