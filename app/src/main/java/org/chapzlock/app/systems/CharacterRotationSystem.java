package org.chapzlock.app.systems;

import java.util.List;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Smoothly rotates the player character to face their movement direction.
 *
 * <p>Uses quaternion slerp (spherical linear interpolation) to avoid gimbal lock
 * and produce natural-looking rotation transitions. The interpolation factor is
 * frame-rate independent: {@code t = min(1, TURN_SMOOTHING * deltaTime)}.
 *
 * <p>When the player is not moving (movement direction is zero), the rotation
 * remains unchanged so the character holds their last facing direction.
 *
 * <p>The target yaw is derived from the XZ movement direction using
 * {@code atan2(dir.x, -dir.z)}, which gives 0 degrees for the -Z axis (forward)
 * and 90 degrees for the +X axis (right).
 */
public class CharacterRotationSystem implements GameSystem {

    private static final float TURN_SMOOTHING = 10f;
    private static final float MODEL_YAW_OFFSET = -90f;

    private ComponentRegistry registry;

    @Override
    public SystemPhase phase() {
        return SystemPhase.LOGIC;
    }

    @Override
    public List<Class<? extends GameSystem>> runAfter() {
        return List.of(CharacterMovementSystem.class);
    }

    @Override
    public List<Class<? extends GameSystem>> runBefore() {
        return List.of(ThirdPersonCameraSystem.class);
    }

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        registry.forEach(PlayerTag.class, Transform.class, CharacterState.class,
            (entityId, tag, transform, state) -> {
                Vector3f dir = state.getMovementDirection();
                if (dir.x == 0f && dir.z == 0f) return;

                float targetYaw = (float) Math.toDegrees(Math.atan2(dir.x, -dir.z)) + MODEL_YAW_OFFSET;

                Quaternionf current = new Quaternionf();
                transform.getRotationQuaternion(current);

                Quaternionf target = new Quaternionf().rotationY((float) Math.toRadians(targetYaw));

                float t = Math.min(1f, TURN_SMOOTHING * deltaTime);
                Quaternionf result = new Quaternionf();
                current.slerp(target, t, result);

                transform.setRotationQuaternion(result);
            });
    }
}
