package org.chapzlock.core.application;

import java.util.List;

import org.chapzlock.core.ecs.GameSystem;

public interface Layer {
    default void onInit(EngineContext ctx) {}
    default List<GameSystem> systems() { return List.of(); }
    default void onDestroy() {}
}
