package org.chapzlock.app.systems;

import java.util.List;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.component.ThirdPersonCamera;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.physics.Physics;
import org.joml.Vector3f;

/**
 * Reads player input and camera yaw to compute world-space movement direction,
 * then sets the player's horizontal velocity via physics while preserving
 * the Y component (gravity/jump).
 *
 * <p>The system translates raw input axes ({@code moveForward}, {@code moveRight})
 * into a world-space direction by rotating them according to the camera's yaw angle.
 * This gives the player "camera-relative" controls: pressing forward always moves
 * in the direction the camera is facing, regardless of the character's own rotation.
 *
 * <p>The horizontal speed is fixed at {@link #MOVE_SPEED}. Diagonal input is normalized
 * so it never exceeds this speed. The vertical (Y) component of velocity is left
 * untouched so gravity and jump impulses are preserved.
 *
 * <p>Runs after {@link CharacterGroundCheckSystem} (which sets grounded state)
 * and before CharacterRotationSystem (which orients the character to face the
 * movement direction).
 */
public class CharacterMovementSystem implements GameSystem {

    private static final float MOVE_SPEED = 8f;

    private ComponentRegistry registry;
    private Physics physics;

    @Override
    public SystemPhase phase() {
        return SystemPhase.LOGIC;
    }

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
        float cameraYaw = getCameraYaw();

        registry.forEach(PlayerTag.class, PlayerInputComponent.class, CharacterState.class,
            (entityId, tag, input, state) -> {
                float forward = input.getMoveForward();
                float right = input.getMoveRight();

                if (forward == 0f && right == 0f) {
                    Vector3f vel = physics.getLinearVelocity(entityId);
                    if (vel != null) {
                        physics.setLinearVelocity(entityId, new Vector3f(0, vel.y, 0));
                    }
                    state.getMovementDirection().set(0, 0, 0);
                    state.setCurrentSpeed(0f);
                    return;
                }

                float yawRad = (float) Math.toRadians(cameraYaw);
                float sinYaw = (float) Math.sin(yawRad);
                float cosYaw = (float) Math.cos(yawRad);

                // Camera forward is (-sinYaw, 0, -cosYaw), camera right is (cosYaw, 0, -sinYaw)
                float dirX = -sinYaw * forward + cosYaw * right;
                float dirZ = -cosYaw * forward - sinYaw * right;

                // Normalize to prevent diagonal speed boost
                float length = (float) Math.sqrt(dirX * dirX + dirZ * dirZ);
                if (length > 0f) {
                    dirX /= length;
                    dirZ /= length;
                }

                Vector3f vel = physics.getLinearVelocity(entityId);
                float currentY = vel != null ? vel.y : 0f;
                physics.setLinearVelocity(entityId,
                    new Vector3f(dirX * MOVE_SPEED, currentY, dirZ * MOVE_SPEED));

                state.getMovementDirection().set(dirX, 0, dirZ);
                state.setCurrentSpeed(MOVE_SPEED);
            });
    }

    private float getCameraYaw() {
        var camOpt = registry.getSingleton(ThirdPersonCamera.class);
        return camOpt.map(ThirdPersonCamera::getYaw).orElse(0f);
    }
}
