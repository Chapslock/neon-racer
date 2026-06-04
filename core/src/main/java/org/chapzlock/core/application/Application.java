package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

import java.util.List;
import java.util.Map;

import org.chapzlock.core.animation.AnimationSystem;
import org.chapzlock.core.asset.gltf.GltfAssetRegistry;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntitySystem;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.TransformSystem;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.physics.PhysicsSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.render.CameraSystem;
import org.chapzlock.core.render.MeshRegistry;
import org.chapzlock.core.render.RenderSystem;
import org.chapzlock.core.render.ShaderRegistry;
import org.chapzlock.core.render.TextureRegistry;
import org.chapzlock.core.scene.SceneManager;
import org.chapzlock.core.tween.TweenManager;
import org.chapzlock.core.tween.TweenSystem;
import org.chapzlock.core.window.Window;

import lombok.Getter;

/**
 * Entry point for the game engine. Creates subsystems and runs the main loop.
 *
 * <p>Usage:
 * <pre>{@code
 * Application.create(ApplicationSpecification.builder()
 *     .windowSpec(WindowSpecification.builder().title("Game").width(1280).height(720).build())
 *     .physicsConfig(PhysicsWorldConfig.withDebug())
 *     .initialScene(myScene)
 *     .build())
 * .run();
 * }</pre>
 */
public class Application {

    private static final Log log = Log.create(Application.class);

    @Getter
    private final ApplicationSpecification appSpec;
    private Engine engine;
    private Window window;
    private SceneManager sceneManager;
    private boolean isRunning = false;

    private Application(ApplicationSpecification spec) {
        this.appSpec = spec;
        ApplicationUtil.init();
    }

    /**
     * Starts the application and enters the main loop. Blocks until the window is closed.
     */
    public void run() {
        log.info("Engine starting: %s (%dx%d)", appSpec.getWindowSpec().getTitle(),
            appSpec.getWindowSpec().getWidth(), appSpec.getWindowSpec().getHeight());

        // Engine context init begin

        EventBus eventBus = new EventBus();
        ComponentRegistry registry = new ComponentRegistry(eventBus);

        try {
            window = new Window(appSpec.getWindowSpec(), eventBus);
            window.create(0);
            log.debug("Window created: %dx%d", appSpec.getWindowSpec().getWidth(), appSpec.getWindowSpec().getHeight());

            ShaderRegistry shaderRegistry = new ShaderRegistry();
            TextureRegistry textureRegistry = new TextureRegistry();
            MeshRegistry meshRegistry = new MeshRegistry();
            Time time = new Time();
            TweenManager tweenManager = new TweenManager();
            GltfAssetRegistry gltfAssetRegistry = new GltfAssetRegistry();

            engine = new Engine(
                registry,
                eventBus,
                meshRegistry,
                textureRegistry,
                shaderRegistry,
                window,
                time,
                tweenManager,
                gltfAssetRegistry,
                java.util.List.of()
            );

            sceneManager = new SceneManager(engine);
            engine.setSceneManager(sceneManager);

            EntitySystem entitySystem = new EntitySystem();
            entitySystem.onInit(engine);
            engine.setEntitySystem(entitySystem);
        } catch (Exception e) {
            if (engine != null) {
                engine.dispose();
            }
            if (window != null) {
                window.destroy();
            }
            throw new RuntimeException("Failed to initialize engine subsystems", e);
        }

        CameraSystem cameraSystem = new CameraSystem(appSpec.getWindowSpec());
        engine.setCameraSystem(cameraSystem);
        List<GameSystem> engineSystems = List.of(
            new TweenSystem(),
            new AnimationSystem(),
            new TransformSystem(),
            new PhysicsSystem(appSpec.getPhysicsConfig(), cameraSystem),
            new RenderSystem()
        );
        sceneManager.registerEngineSystems(engineSystems);
        engineSystems.forEach(system -> system.onInit(engine));

        if (appSpec.getInitialScene() != null) {
            sceneManager.loadImmediate(appSpec.getInitialScene());
        } else {
            log.warn("No active scene set!");
        }

        logActiveSystems(sceneManager.getActiveSystems());
        mainLoop();

        log.info("--- Shutdown ---");
        sceneManager.unloadAll();
        engineSystems.forEach(GameSystem::onDestroy);
        engine.dispose();
        window.destroy();
        ApplicationUtil.terminate();
    }

    private void mainLoop() {
        log.info("--- Main loop started ---");
        isRunning = true;
        float lastTime = getTime();

        while (isRunning) {
            glfwPollEvents();

            float now = getTime();
            float rawDelta = now - lastTime;
            lastTime = now;

            if (window.shouldClose()) {
                stop();
                break;
            }

            engine.getTime().update(rawDelta);
            float delta = engine.getTime().getDelta();

            ApplicationUtil.clearAndPrepareWindowState();

            List<GameSystem> activeSystems = sceneManager.getActiveSystems();
            for (GameSystem system : activeSystems) {
                system.onUpdate(delta);
            }
            for (GameSystem system : activeSystems) {
                system.onRender(delta);
            }
            for (GameSystem system : activeSystems) {
                system.onDebugRender(delta);
            }

            InputMap activeInputMap = engine.getActiveInputMap();
            if (activeInputMap != null) {
                activeInputMap.frameReset();
            }

            sceneManager.processPendingTransitions();

            glfwSwapBuffers(window.getId());
        }
    }

    private void logActiveSystems(List<GameSystem> systems) {
        Map<SystemPhase, Long> phaseCounts = systems.stream()
            .collect(java.util.stream.Collectors.groupingBy(GameSystem::phase, java.util.stream.Collectors.counting()));
        String phaseSummary = phaseCounts.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(java.util.stream.Collectors.joining(", "));
        log.info("Systems active: %d (%s)", systems.size(), phaseSummary);
        for (GameSystem system : systems) {
            log.debug("  [%s] %s", system.phase(), system.getClass().getSimpleName());
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

    private void stop() {
        isRunning = false;
    }

    /**
     * Creates a new application instance.
     *
     * @param spec application configuration
     * @return a new application instance
     */
    public static Application create(ApplicationSpecification spec) {
        return new Application(spec);
    }
}
