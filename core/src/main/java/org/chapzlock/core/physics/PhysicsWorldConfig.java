package org.chapzlock.core.physics;

import org.joml.Vector3f;

/**
 * Configuration for the physics simulation world.
 *
 * <h3>What these values mean</h3>
 * <ul>
 *   <li><strong>gravity</strong> — acceleration applied to all dynamic bodies every frame.
 *       Earth gravity is (0, -9.81, 0). Set to (0,0,0) for zero-gravity (space games).
 *       The Y axis points up in this engine.</li>
 *   <li><strong>fixedTimeStep</strong> — the physics engine advances in fixed-size steps
 *       (default: 1/60 second = 60Hz). This is independent of your rendering frame rate.
 *       Smaller values = more precise but more CPU-expensive. 1/60 is industry standard.</li>
 *   <li><strong>maxSubSteps</strong> — if a frame takes longer than fixedTimeStep, the
 *       physics engine catches up by running multiple steps. This caps how many catch-up
 *       steps are allowed. Set to 10 = handles frame rates down to ~6fps. If your game
 *       drops below that, physics starts running in slow motion rather than exploding.</li>
 *   <li><strong>worldBoundsMin/Max</strong> — the axis-aligned bounding box of the physics
 *       world. Objects outside these bounds are not tracked by the broadphase collision
 *       detection. Keep this as small as your game world allows for better performance.</li>
 *   <li><strong>debugEnabled</strong> — when true, the physics system renders wireframe
 *       overlays showing collision shapes, contact points, and AABBs. Expensive — use
 *       only during development.</li>
 * </ul>
 */
public record PhysicsWorldConfig(
    Vector3f gravity,
    float fixedTimeStep,
    int maxSubSteps,
    Vector3f worldBoundsMin,
    Vector3f worldBoundsMax,
    boolean debugEnabled
) {
    public static PhysicsWorldConfig defaults() {
        return new PhysicsWorldConfig(
            new Vector3f(0f, -9.81f, 0f),
            1f / 60f,
            10,
            new Vector3f(-1000f, -1000f, -1000f),
            new Vector3f(1000f, 1000f, 1000f),
            false
        );
    }

    public static PhysicsWorldConfig withDebug() {
        return new PhysicsWorldConfig(
            new Vector3f(0f, -9.81f, 0f),
            1f / 60f,
            10,
            new Vector3f(-1000f, -1000f, -1000f),
            new Vector3f(1000f, 1000f, 1000f),
            true
        );
    }
}
