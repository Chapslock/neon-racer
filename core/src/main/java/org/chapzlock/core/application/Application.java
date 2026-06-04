package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

import java.util.List;

import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.logging.Log;

/**
 * Entry point for the game engine. Creates the engine and runs the main loop.
 *
 * <p>Usage:
 * <pre>{@code
 * Application.run(ApplicationSpecification.builder()
 *     .windowSpec(WindowSpecification.builder().title("Game").width(1280).height(720).build())
 *     .physicsConfig(PhysicsWorldConfig.withDebug())
 *     .initialScene(myScene)
 *     .build());
 * }</pre>
 */
public class Application {

    private static final Log log = Log.create(Application.class);

    /**
     * Creates the engine, loads the initial scene, and enters the main loop.
     * Blocks until the window is closed.
     *
     * @param spec application configuration (window, physics, initial scene)
     */
    public static void run(ApplicationSpecification spec) {
        Engine engine = Engine.create(spec);

        if (spec.getInitialScene() != null) {
            engine.getSceneManager().loadImmediate(spec.getInitialScene());
        } else {
            log.warn("No initial scene set!");
        }

        logActiveSystems(engine);
        log.info("--- Main loop started ---");

        mainLoop(engine);

        log.info("--- Shutdown ---");
        engine.dispose();
        ApplicationUtil.terminate();
    }

    private static void mainLoop(Engine engine) {
        float lastTime = getTime();

        while (true) {
            glfwPollEvents();

            float now = getTime();
            float rawDelta = now - lastTime;
            lastTime = now;

            if (engine.getWindow().shouldClose()) {
                break;
            }

            engine.getTime().update(rawDelta);
            float delta = engine.getTime().getDelta();

            ApplicationUtil.clearAndPrepareWindowState();

            List<GameSystem> activeSystems = engine.getSceneManager().getActiveSystems();
            for (GameSystem system : activeSystems) {
                system.onUpdate(delta);
            }
            for (GameSystem system : activeSystems) {
                system.onRender(delta);
            }
            for (GameSystem system : activeSystems) {
                system.onDebugRender(delta);
            }

            InputMap activeInputMap = engine.getSceneManager().getActiveInputMap();
            if (activeInputMap != null) {
                activeInputMap.frameReset();
            }

            engine.getSceneManager().processPendingTransitions();

            glfwSwapBuffers(engine.getWindow().getId());
        }
    }

    private static void logActiveSystems(Engine engine) {
        List<GameSystem> systems = engine.getSceneManager().getActiveSystems();
        log.info("Systems active: %d", systems.size());
        if (log.isDebugEnabled()) {
            for (GameSystem system : systems) {
                log.debug("  [%s] %s", system.phase(), system.getClass().getSimpleName());
            }
        }
    }

    /**
     * Returns GLFW time in seconds since application start.
     *
     * @return elapsed time in seconds
     */
    public static float getTime() {
        return (float) glfwGetTime();
    }
}
