package org.chapzlock.core.ecs;

import org.chapzlock.core.application.EngineContext;

/**
 * A system that participates in the engine's update loop.
 *
 * <p>Lifecycle: {@code onInit} is called once when registered.
 * {@code onUpdate}, {@code onRender}, and {@code onDebugRender} run every frame.
 * {@code onDestroy} is called when the scene unloads or at shutdown.
 *
 * <p>To react to scene transitions, subscribe to {@code SceneLoadedEvent} in {@code onInit()}.
 *
 * <p>Example:
 * <pre>{@code
 * public class MySystem implements GameSystem {
 *     private ComponentRegistry registry;
 *
 *     @Override
 *     public void onInit(EngineContext ctx) {
 *         this.registry = ctx.getRegistry();
 *     }
 *
 *     @Override
 *     public void onUpdate(float deltaTime) {
 *         registry.forEach(Transform.class, Velocity.class, (entityId, transform, vel) -> {
 *             // Update logic
 *         });
 *     }
 * }
 * }</pre>
 */
public interface GameSystem {

    /**
     * Returns the execution phase for this system. Defaults to {@link SystemPhase#LOGIC}.
     */
    default SystemPhase phase() { return SystemPhase.LOGIC; }

    /**
     * Called once when the system is registered. Use to store references from context.
     *
     * @param ctx the engine context containing all subsystems
     */
    default void onInit(EngineContext ctx) {}

    /**
     * Called every frame for game logic updates.
     *
     * @param deltaTime time since last frame in seconds
     */
    default void onUpdate(float deltaTime) {}

    /**
     * Called every frame for rendering. Only implement if your system renders entities.
     *
     * @param deltaTime time since last frame in seconds
     */
    default void onRender(float deltaTime) {}

    /**
     * Called every frame for debug visualization (physics shapes, gizmos, etc).
     *
     * @param deltaTime time since last frame in seconds
     */
    default void onDebugRender(float deltaTime) {}

    /**
     * Called when the system is removed (scene unload or shutdown). Use to cleanup resources.
     */
    default void onDestroy() {}

}
