package org.chapzlock.app.systems;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityView;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.window.Window;

import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

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

        for (EntityView e : registry.view(PlayerTag.class, PlayerInputComponent.class)) {
            var playerInput = e.get(PlayerInputComponent.class);
            playerInput.setThrottle(input.getAxis("throttle"));
            playerInput.setSteer(input.getAxis("steer"));
        }
    }
}
