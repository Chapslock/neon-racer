package org.chapzlock.app.systems;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.EntityView;
import org.chapzlock.core.physics.PhysicsForceUtil;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.joml.Vector3f;

public class PlayerMovementSystem implements GameSystem {
    private ComponentRegistry registry;

    private static final float ENGINE_FORCE = 1200f;
    private static final float STEER_TORQUE = 1000f;

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView e : registry.view(PlayerTag.class, PhysicsBody.class, PlayerInputComponent.class, Transform.class)) {
            var input = e.get(PlayerInputComponent.class);
            var physicsBody = e.get(PhysicsBody.class);

            if (input.getThrottle() != 0f) {
                float localForceMagnitude = ENGINE_FORCE * input.getThrottle() * deltaTime;
                PhysicsForceUtil.applyLocalForce(physicsBody.getRigidBody(), new Vector3f(0f, 0f, localForceMagnitude));
            }

            if (input.getSteer() != 0f) {
                float steerAmount = STEER_TORQUE * -input.getSteer() * deltaTime;
                PhysicsForceUtil.applyLocalTorque(physicsBody.getRigidBody(), new Vector3f(0f, steerAmount, 0f));
            }
        }
    }
}
