package org.chapzlock.app.systems;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.System;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.registry.ComponentRegistry;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerMovementSystem implements System {
    private final ComponentRegistry registry;

    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView e : registry.view(PlayerTag.class, Transform.class, PlayerInputComponent.class)) {
            var input = e.get(PlayerInputComponent.class);
            var transform = e.get(Transform.class);

            if (input.isMovingForward()) {
                transform.getPosition().y += deltaTime;
            }
            if (input.isMovingBackwards()) {
                transform.getPosition().y -= deltaTime;
            }
            if (input.isMovingLeft()) {
                transform.getPosition().x -= deltaTime;
            }
            if (input.isMovingRight()) {
                transform.getPosition().x += deltaTime;
            }
        }
    }
}
