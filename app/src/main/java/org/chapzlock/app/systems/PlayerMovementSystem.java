package org.chapzlock.app.systems;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.System;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.physics.PhysicsForceUtil;
import org.chapzlock.core.registry.ComponentRegistry;
import org.joml.Vector3f;

public class PlayerMovementSystem implements System {
    private final ComponentRegistry registry = ComponentRegistry.instance();

    private static final float ENGINE_FORCE = 800f;    // Newtons applied in local forward
    private static final float BRAKE_FORCE = 1000f;     // stronger braking
    private static final float STEER_TORQUE = 1000f;    // torque around local up for yaw
    private static final float ROLL_DAMPING = 0.2f;     // optional damping factor (not used here)

    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView e : registry.view(PlayerTag.class, PhysicsBody.class, PlayerInputComponent.class, Transform.class)) {
            var input = e.get(PlayerInputComponent.class);
            var physicsBody = e.get(PhysicsBody.class);
            // Transform isn't used for computing local forces here - the physics body orientation is used
            // by applyLocalForce/applyLocalTorque so we avoid mixing local/world spaces.

            // Throttle / brake
            if (input.isMovingForward() || input.isMovingBackwards()) {
                // positive local Z is forward; negative is reverse/brake
                float base = input.isMovingBackwards() ? -BRAKE_FORCE : ENGINE_FORCE;
                float localForceMagnitude = base * deltaTime;
                // Local-space forward vector: (0, 0, +Z)
                Vector3f localForce = new Vector3f(0f, 0f, localForceMagnitude);
                PhysicsForceUtil.applyLocalForce(physicsBody.getRigidBody(), localForce);
            }

            // Steering: apply torque around the vehicle's local up axis (local Y)
            if (input.isMovingLeft() || input.isMovingRight()) {
                float steerDir = input.isMovingLeft() ? 1f : -1f; // tune sign if it feels inverted
                float steerAmount = STEER_TORQUE * steerDir * deltaTime;
                // Local-space torque around the up axis: (0, +Y, 0)
                Vector3f localTorque = new Vector3f(0f, steerAmount, 0f);
                PhysicsForceUtil.applyLocalTorque(physicsBody.getRigidBody(), localTorque);
            }

            // Optional: lateral damping, traction, angular velocity clamping here
        }
    }
}
