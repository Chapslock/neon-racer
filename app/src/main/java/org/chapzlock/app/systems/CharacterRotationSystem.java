package org.chapzlock.app.systems;

import java.util.List;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.component.ThirdPersonCamera;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.input.InputMap;
import org.joml.Quaternionf;

/**
 * Controls player facing direction via two modes:
 *
 * <ol>
 *   <li><strong>Keyboard turning</strong> — when A/D keys are pressed (via "turnRight" axis),
 *       the player rotates at TURN_SPEED degrees/second. Only active when right-click orbit
 *       is not held.</li>
 *   <li><strong>Camera snap</strong> — on right-click release, the player's facing direction
 *       snaps to match the camera's yaw. This lets the player orbit the camera with right-click
 *       and then run in that direction when released.</li>
 * </ol>
 *
 * <p>Tracks {@code wasOrbitingRight} state across frames to detect the right-click
 * release transition. Runs after {@link CharacterMovementSystem} and before
 * {@link ThirdPersonCameraSystem}.
 */
public class CharacterRotationSystem implements GameSystem {

    private static final float TURN_SPEED = 180f;

    private ComponentRegistry registry;
    private Engine engine;
    private boolean wasOrbitingRight;

    @Override
    public SystemPhase phase() { return SystemPhase.LOGIC; }

    @Override
    public List<Class<? extends GameSystem>> runAfter() {
        return List.of(CharacterMovementSystem.class);
    }

    @Override
    public List<Class<? extends GameSystem>> runBefore() {
        return List.of(ThirdPersonCameraSystem.class);
    }

    @Override
    public void onInit(Engine engine) {
        this.engine = engine;
        this.registry = engine.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        InputMap input = engine.getActiveInputMap();
        if (input == null) return;

        var camOpt = registry.getSingleton(ThirdPersonCamera.class);
        boolean orbitingRight = camOpt.map(ThirdPersonCamera::isOrbitingRight).orElse(false);

        // On right-click release, snap player facing to camera direction
        if (wasOrbitingRight && !orbitingRight) {
            camOpt.ifPresent(tpc -> {
                float cameraFacingYaw = tpc.getYaw() + 90f;
                registry.forEach(PlayerTag.class, Transform.class, CharacterState.class,
                    (entityId, tag, transform, state) -> {
                        state.setFacingYaw(cameraFacingYaw);
                        transform.setRotationQuaternion(
                            new Quaternionf().rotationY((float) Math.toRadians(cameraFacingYaw)));
                    });
            });
        }
        wasOrbitingRight = orbitingRight;

        // A/D keyboard turning (only when not orbiting with right-click)
        if (!orbitingRight) {
            float turnInput = input.getAxis("turnRight");
            if (turnInput != 0f) {
                float turnAmount = -turnInput * TURN_SPEED * deltaTime;
                registry.forEach(PlayerTag.class, Transform.class, CharacterState.class,
                    (entityId, tag, transform, state) -> {
                        float newYaw = state.getFacingYaw() + turnAmount;
                        state.setFacingYaw(newYaw);
                        transform.setRotationQuaternion(
                            new Quaternionf().rotationY((float) Math.toRadians(newYaw)));
                    });
            }
        }
    }
}
