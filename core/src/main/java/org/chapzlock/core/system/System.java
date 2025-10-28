package org.chapzlock.core.system;

/**
 * System that needs to be a part of the application lifecycle
 */
public interface System {

    default void onUpdate(float deltaTime) {}

    default void onRender(float deltaTime) {}

    default void onInit() {}

    default void onDestroy() {}

}
