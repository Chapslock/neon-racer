package org.chapzlock.application.systems;

import org.chapzlock.application.component.InputComponent;
import org.chapzlock.application.tags.PlayerTag;
import org.chapzlock.core.component.TransformComponent;
import org.chapzlock.core.component.orchestration.ComponentRegistry;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.system.System;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerMovementSystem implements System {
    private final ComponentRegistry registry;

    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView e : registry.view(PlayerTag.class, TransformComponent.class, InputComponent.class)) {
            var input = e.get(InputComponent.class);
            var transform = e.get(TransformComponent.class);
            float playerSpeed = deltaTime;

            if (input.isMovingForward()) {
                transform.getPosition().y += playerSpeed;
            }
            if (input.isMovingBackwards()) {
                transform.getPosition().y -= playerSpeed;
            }
            if (input.isMovingLeft()) {
                transform.getPosition().x -= playerSpeed;
            }
            if (input.isMovingRight()) {
                transform.getPosition().x += playerSpeed;
            }
        }
    }
}
