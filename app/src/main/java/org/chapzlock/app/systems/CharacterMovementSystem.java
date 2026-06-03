package org.chapzlock.app.systems;

import java.util.List;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.physics.Physics;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Moves the player character based on input and current facing direction.
 *
 * <p>Transforms the forward input value (-1 to 1) into a world-space velocity by
 * rotating the model's local +X axis by the player's current quaternion rotation.
 * The Y component of velocity is preserved from physics (gravity) so movement
 * doesn't fight vertical forces.
 *
 * <p>When input is zero, horizontal velocity is zeroed immediately (no momentum).
 * This gives responsive, arcade-style movement rather than realistic deceleration.
 *
 * <p>Depends on {@link CharacterGroundCheckSystem} (runs after it) and runs before
 * {@link CharacterRotationSystem}.
 */
public class CharacterMovementSystem implements GameSystem {

    private static final float MOVE_SPEED = 8f;

    private ComponentRegistry registry;
    private Physics physics;

    @Override
    public SystemPhase phase() { return SystemPhase.LOGIC; }

    @Override
    public List<Class<? extends GameSystem>> runAfter() {
        return List.of(CharacterGroundCheckSystem.class);
    }

    @Override
    public List<Class<? extends GameSystem>> runBefore() {
        return List.of(CharacterRotationSystem.class);
    }

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.physics = ctx.getPhysics();
    }

    @Override
    public void onUpdate(float deltaTime) {
        registry.forEach(PlayerTag.class, PlayerInputComponent.class, CharacterState.class,
            Transform.class, (entityId, tag, input, state, transform) -> {
                float forward = input.getMoveForward();

                if (forward == 0f) {
                    Vector3f vel = physics.getLinearVelocity(entityId);
                    if (vel != null) {
                        physics.setLinearVelocity(entityId, new Vector3f(0, vel.y, 0));
                    }
                    state.getMovementDirection().set(0, 0, 0);
                    state.setCurrentSpeed(0f);
                    return;
                }

                // Get player's facing direction from current rotation
                Quaternionf rotation = new Quaternionf();
                transform.getRotationQuaternion(rotation);

                // Model faces +X, so transform +X by the rotation to get world forward
                Vector3f worldForward = new Vector3f(1, 0, 0);
                rotation.transform(worldForward);
                worldForward.y = 0f;
                worldForward.normalize();

                float dirX = worldForward.x * forward;
                float dirZ = worldForward.z * forward;

                Vector3f vel = physics.getLinearVelocity(entityId);
                float currentY = vel != null ? vel.y : 0f;
                physics.setLinearVelocity(entityId,
                    new Vector3f(dirX * MOVE_SPEED, currentY, dirZ * MOVE_SPEED));

                state.getMovementDirection().set(dirX, 0, dirZ);
                state.setCurrentSpeed(MOVE_SPEED);
            });
    }
}
