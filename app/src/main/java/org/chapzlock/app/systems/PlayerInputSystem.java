package org.chapzlock.app.systems;

import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.window.Window;

public class PlayerInputSystem implements GameSystem {

    private ComponentRegistry registry;
    private EngineContext context;

    @Override
    public SystemPhase phase() { return SystemPhase.INPUT; }

    @Override
    public void onInit(EngineContext ctx) {
        this.context = ctx;
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        InputMap input = context.getActiveInputMap();
        if (input == null) return;

        if (input.wasJustPressed("quit")) {
            Window window = context.getWindow();
            glfwSetWindowShouldClose(window.getId(), true);
            return;
        }

        registry.forEach(PlayerTag.class, PlayerInputComponent.class, (entityId, tag, playerInput) -> {
            playerInput.setMoveForward(input.getAxis("moveForward"));
        });
    }
}
