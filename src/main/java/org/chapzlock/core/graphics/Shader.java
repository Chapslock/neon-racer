package org.chapzlock.core.graphics;

import org.chapzlock.core.component.LightComponent;
import org.joml.Matrix4f;

public interface Shader {
    void bind();
    void unbind();
    void onDestroy();

    default void loadTransformationMatrix(Matrix4f matrix4f) {
    }

    default void loadProjectionMatrix(Matrix4f matrix4f) {
    }

    default void loadViewMatrix(Matrix4f matrix4f) {
    }

    default void loadLight(LightComponent light) {
    }

    default void loadShine(float shineDamper, float reflectivity) {
    }

}
