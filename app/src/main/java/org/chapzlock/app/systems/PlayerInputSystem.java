package org.chapzlock.app.systems;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.System;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.input.InputUtil;
import org.chapzlock.core.registry.ComponentRegistry;
import org.lwjgl.glfw.GLFW;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerInputSystem implements System {

    private final ComponentRegistry registry;

    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView e : registry.view(PlayerTag.class, PlayerInputComponent.class)) {
            var input = e.get(PlayerInputComponent.class);
            input.setMovingLeft(InputUtil.isKeyPressed(GLFW.GLFW_KEY_A));
            input.setMovingRight(InputUtil.isKeyPressed(GLFW.GLFW_KEY_D));
            input.setMovingForward(InputUtil.isKeyPressed(GLFW.GLFW_KEY_W));
            input.setMovingBackwards(InputUtil.isKeyPressed(GLFW.GLFW_KEY_S));
        }
    }
}
