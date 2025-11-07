package org.chapzlock.core.application;

public interface Layer {
    default void onUpdate(float deltaTime) {
    }
    default void onRender(float deltaTime) {}
    default void onDestroy(){}

    default void onInit() {
    }
}
