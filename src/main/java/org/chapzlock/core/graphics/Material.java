package org.chapzlock.core.graphics;

import org.chapzlock.core.component.ReflectionComponent;

public interface Material {

    void bind();
    void unbind();
    Shader getShader();

    ReflectionComponent getReflection();
}
