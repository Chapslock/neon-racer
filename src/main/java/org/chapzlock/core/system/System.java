package org.chapzlock.core.system;

public interface System {

    default void onUpdate(float deltaTime) {}

    default void onRender(float deltaTime) {}

    default void onInit() {}

    default void onDestroy() {}

}
