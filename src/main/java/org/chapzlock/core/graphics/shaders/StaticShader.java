package org.chapzlock.core.graphics.shaders;

import org.chapzlock.core.graphics.ShaderProgram;

public class StaticShader extends ShaderProgram {

    private static final String VERTEX_FILE = "shaders/debug.vert.glsl";
    private static final String FRAGMENT_FILE = "shaders/debug.frag.glsl";

    public StaticShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }
}
