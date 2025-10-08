package org.chapzlock.core.graphics;

public interface Material {

    void bind();
    void unbind();
    Shader getShader();
}
