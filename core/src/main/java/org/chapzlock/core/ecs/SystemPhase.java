package org.chapzlock.core.ecs;

/**
 * Execution phases for game systems.
 *
 * <p>Systems are sorted by phase and execute in this order each frame:
 * {@link #INPUT} → {@link #LOGIC} → {@link #PHYSICS} → {@link #RENDER}.
 */
public enum SystemPhase {
    /** Input processing (key/mouse events, InputMap updates). */
    INPUT,
    /** Game logic (AI, spawning, timers). Default phase. */
    LOGIC,
    /** Physics simulation (collision, forces). */
    PHYSICS,
    /** Rendering (batching, GPU draw calls). */
    RENDER
}
