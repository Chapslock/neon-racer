package org.chapzlock.app.systems;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.System;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.input.keyboard.KeyPressedEvent;
import org.chapzlock.core.input.keyboard.KeyReleasedEvent;
import org.chapzlock.core.registry.ComponentRegistry;
import org.lwjgl.glfw.GLFW;

public class PlayerInputSystem implements System {

    private final ComponentRegistry registry = ComponentRegistry.instance();
    private final EventBus eventBus = EventBus.instance();
    private final List<Subscription> subscriptions = new ArrayList<>();


    @Override
    public void onInit() {
        this.subscriptions.add(eventBus.subscribe(KeyPressedEvent.class, this::handleKeyPress));
        this.subscriptions.add(eventBus.subscribe(KeyReleasedEvent.class, this::handleKeyRelease));
    }

    @Override
    public void onDestroy() {
        this.subscriptions.forEach(Subscription::unsubscribe);
    }

    private boolean handleKeyPress(KeyPressedEvent event) {
        for (EntityView e : registry.view(PlayerTag.class, PlayerInputComponent.class)) {
            var input = e.get(PlayerInputComponent.class);
            switch (event.key()) {
                case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> input.setMovingForward(true);
                case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> input.setMovingBackwards(true);
                case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> input.setMovingLeft(true);
                case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> input.setMovingRight(true);
            }
        }
        return false;
    }

    private boolean handleKeyRelease(KeyReleasedEvent event) {
        for (EntityView e : registry.view(PlayerTag.class, PlayerInputComponent.class)) {
            var input = e.get(PlayerInputComponent.class);
            switch (event.keyCode()) {
                case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> input.setMovingForward(false);
                case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> input.setMovingBackwards(false);
                case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> input.setMovingLeft(false);
                case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> input.setMovingRight(false);
            }
        }
        return false;
    }
}
