package org.chapzlock.app.systems;

import java.util.List;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.component.ThirdPersonCamera;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.input.InputMap;
import org.joml.Quaternionf;

public class CharacterRotationSystem implements GameSystem {

    private static final float TURN_SPEED = 180f;

    private ComponentRegistry registry;
    private EngineContext context;
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
    public void onInit(EngineContext ctx) {
        this.context = ctx;
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        InputMap input = context.getActiveInputMap();
        if (input == null) return;

        var camOpt = registry.getSingleton(ThirdPersonCamera.class);
        boolean orbitingRight = camOpt.map(ThirdPersonCamera::isOrbitingRight).orElse(false);

        // On right-click release, snap player facing to camera direction
        if (wasOrbitingRight && !orbitingRight) {
            camOpt.ifPresent(tpc -> {
                float cameraFacingYaw = tpc.getYaw() + 90f;
                float yawRad = (float) Math.toRadians(cameraFacingYaw);
                registry.forEach(PlayerTag.class, Transform.class, CharacterState.class,
                    (entityId, tag, transform, state) -> {
                        transform.setRotationQuaternion(new Quaternionf().rotationY(yawRad));
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
                        Quaternionf current = new Quaternionf();
                        transform.getRotationQuaternion(current);
                        Quaternionf turn = new Quaternionf().rotationY((float) Math.toRadians(turnAmount));
                        current.mul(turn);
                        transform.setRotationQuaternion(current);
                    });
            }
        }
    }
}
