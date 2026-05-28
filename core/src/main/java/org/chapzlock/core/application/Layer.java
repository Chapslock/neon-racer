package org.chapzlock.core.application;

public interface Layer {
    default void onInit(EngineContext ctx) {}
    default void onUpdate(float deltaTime) {}
    default void onRender(float deltaTime) {}
    default void onDestroy() {}
}
