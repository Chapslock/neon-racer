package org.chapzlock.application.systems;

import org.chapzlock.application.component.InputComponent;
import org.chapzlock.application.tags.PlayerTag;
import org.chapzlock.core.component.orchestration.ComponentRegistry;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.input.Input;
import org.chapzlock.core.system.System;
import org.lwjgl.glfw.GLFW;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerInputSystem implements System {

    private final ComponentRegistry registry;

    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView e : registry.view(PlayerTag.class, InputComponent.class)) {
            var input = e.get(InputComponent.class);
            input.setMovingLeft(Input.isKeyPressed(GLFW.GLFW_KEY_A));
            input.setMovingRight(Input.isKeyPressed(GLFW.GLFW_KEY_D));
            input.setMovingForward(Input.isKeyPressed(GLFW.GLFW_KEY_W));
            input.setMovingBackwards(Input.isKeyPressed(GLFW.GLFW_KEY_S));
        }
    }
}
