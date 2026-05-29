package org.chapzlock.core.ecs;

import org.chapzlock.core.application.EngineContext;

/**
 * System that needs to be a part of the application lifecycle
 */
public interface GameSystem {

    default SystemPhase phase() { return SystemPhase.LOGIC; }

    default void onInit(EngineContext ctx) {}

    default void onUpdate(float deltaTime) {}

    default void onRender(float deltaTime) {}

    default void onDebugRender(float deltaTime) {}

    default void onDestroy() {}

}
