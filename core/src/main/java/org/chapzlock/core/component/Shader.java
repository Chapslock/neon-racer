package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;

/**
 * Data-only shader component: stores source paths and program id once compiled.
 *
 */
public class Shader implements Component {
    private final String vertexPath;
    private final String fragmentPath;
    private int programId;

    public Shader(String vertexPath, String fragmentPath) {
        this.vertexPath = vertexPath;
        this.fragmentPath = fragmentPath;
        this.programId = 0;
    }

    public String getVertexPath() {
        return vertexPath;
    }

    public String getFragmentPath() {
        return fragmentPath;
    }

    public int getProgramId() {
        return programId;
    }

    public void setProgramId(int programId) {
        this.programId = programId;
    }

    public boolean isCompiled() {
        return programId != 0;
    }
}
