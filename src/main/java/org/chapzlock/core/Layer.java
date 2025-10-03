package org.chapzlock.core;

public interface Layer {

    default void onEvent(Event e) {};
    default void onUpdate(float timeStep) {};
    default void onRender() {}
}
