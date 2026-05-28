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

    private static final float ENGINE_FORCE = 1200f;    // Newtons applied in local forward
    private static final float BRAKE_FORCE = 1000f;     // stronger braking
    private static final float STEER_TORQUE = 1000f;    // torque around local up for yaw
    private static final float ROLL_DAMPING = 0.2f;     // optional damping factor (not used here)

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView e : registry.view(PlayerTag.class, PhysicsBody.class, PlayerInputComponent.class, Transform.class)) {
            var input = e.get(PlayerInputComponent.class);
            var physicsBody = e.get(PhysicsBody.class);

            // Throttle / brake
            if (input.isMovingForward() || input.isMovingBackwards()) {
                float base = input.isMovingBackwards() ? -BRAKE_FORCE : ENGINE_FORCE;
                float localForceMagnitude = base * deltaTime;
                Vector3f localForce = new Vector3f(0f, 0f, localForceMagnitude);
                PhysicsForceUtil.applyLocalForce(physicsBody.getRigidBody(), localForce);
            }

            // Steering: apply torque around the vehicle's local up axis (local Y)
            if (input.isMovingLeft() || input.isMovingRight()) {
                float steerDir = input.isMovingLeft() ? 1f : -1f;
                float steerAmount = STEER_TORQUE * steerDir * deltaTime;
                Vector3f localTorque = new Vector3f(0f, steerAmount, 0f);
                PhysicsForceUtil.applyLocalTorque(physicsBody.getRigidBody(), localTorque);
            }
        }
    }
}
