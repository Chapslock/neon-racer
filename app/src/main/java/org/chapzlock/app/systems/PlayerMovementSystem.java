package org.chapzlock.app.systems;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.System;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.physics.PhysicsForceUtil;
import org.chapzlock.core.registry.ComponentRegistry;

public class PlayerMovementSystem implements System {
    private final ComponentRegistry registry = ComponentRegistry.instance();

    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView e : registry.view(PlayerTag.class, PhysicsBody.class, PlayerInputComponent.class, Transform.class)) {
            var input = e.get(PlayerInputComponent.class);
            var physicsBody = e.get(PhysicsBody.class);
            var transform = e.get(Transform.class);

            if (input.isMovingForward()) {
                PhysicsForceUtil.applyLocalForce(physicsBody.getRigidBody(), transform.getForwardVector());
            }
            if (input.isMovingBackwards()) {
                PhysicsForceUtil.applyLocalForce(physicsBody.getRigidBody(), transform.getForwardVector().negate());
            }
            if (input.isMovingLeft()) {
                PhysicsForceUtil.applyLocalForce(physicsBody.getRigidBody(), transform.getUpVector()
                    .cross(transform.getForwardVector())
                    .negate());
            }
            if (input.isMovingRight()) {
                PhysicsForceUtil.applyLocalForce(physicsBody.getRigidBody(), transform.getUpVector().cross(transform.getForwardVector()));
            }
        }
    }
}
