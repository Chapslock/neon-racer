package org.chapzlock.core.graphics;

import org.chapzlock.core.math.Matrix4f;

public interface Shader {
    void bind();
    void unbind();
    void onDestroy();
    default void loadTransformationMatrix(Matrix4f matrix4f) {};
    default void loadProjectionMatrix(Matrix4f matrix4f) {};
    default void loadViewMatrix(Matrix4f matrix4f) {};

}
