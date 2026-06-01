package org.chapzlock.app.systems;

import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityView;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.window.Window;

/**
 * Reads raw keyboard input from the active {@link InputMap} and writes normalized
 * movement values into the player's {@link PlayerInputComponent}.
 *
 * <p>Runs in the {@link SystemPhase#INPUT} phase so that movement systems in later
 * phases always see fresh input for the current frame.
 *
 * <p>Input mapping:
 * <ul>
 *   <li>"moveForward" axis: W (+1) / S (-1)</li>
 *   <li>"moveRight" axis: D (+1) / A (-1)</li>
 *   <li>"quit" action: Escape key closes the window</li>
 * </ul>
 */
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
            playerInput.setMoveForward(input.getAxis("moveForward"));
            playerInput.setMoveRight(input.getAxis("moveRight"));
        }
    }
}
