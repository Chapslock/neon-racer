package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

import java.util.List;

import org.chapzlock.core.asset.gltf.GltfAssetRegistry;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntitySystem;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.render.CameraSystem;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.scene.SceneManager;
import org.chapzlock.core.tween.TweenManager;
import org.chapzlock.core.window.Window;
import org.chapzlock.core.window.WindowSpecification;

import lombok.Getter;

/**
 * Entry point for the game engine. Creates subsystems and runs the main loop.
 *
 * <p>Usage:
 * <pre>{@code
 * Application.create(ApplicationSpecification.builder()
 *     .windowSpec(WindowSpecification.builder().title("Game").width(1280).height(720).build())
 *     .engineSystems(List.of(new PhysicsSystem(), new RenderSystem()))
 *     .initialScene(myScene)
 *     .build())
 * .run();
 * }</pre>
 */
public class Application {
    @Getter
    private final ApplicationSpecification appSpec;
    private EngineContext context;
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
        createSubsystems();

        List<GameSystem> engineSystems = appSpec.getEngineSystems();
        sceneManager.registerEngineSystems(engineSystems);
        engineSystems.forEach(system -> system.onInit(context));

        if (appSpec.getInitialScene() != null) {
            sceneManager.loadImmediate(appSpec.getInitialScene());
        }

        logActiveSystems(sceneManager.getActiveSystems());
        mainLoop();

        sceneManager.unloadAll();
        engineSystems.forEach(GameSystem::onDestroy);
        context.dispose();
        window.destroy();
        ApplicationUtil.terminate();
    }

    private void createSubsystems() {
        EventBus eventBus = new EventBus();
        ComponentRegistry registry = new ComponentRegistry(eventBus);

        window = new Window(appSpec.getWindowSpec(), eventBus);
        window.create(0);
        Log.debug("Window created: " + appSpec.getWindowSpec().getWidth() + "x" + appSpec.getWindowSpec().getHeight());

        ShaderSystem shaderSystem = new ShaderSystem();
        TextureSystem textureSystem = new TextureSystem();
        MeshSystem meshSystem = new MeshSystem();
        CameraSystem cameraSystem = new CameraSystem(appSpec.getWindowSpec());
        Time time = new Time();
        TweenManager tweenManager = new TweenManager();
        GltfAssetRegistry gltfAssetRegistry = new GltfAssetRegistry();

        context = EngineContext.builder()
            .registry(registry)
            .eventBus(eventBus)
            .meshSystem(meshSystem)
            .textureSystem(textureSystem)
            .shaderSystem(shaderSystem)
            .cameraSystem(cameraSystem)
            .window(window)
            .time(time)
            .tweenManager(tweenManager)
            .gltfAssetRegistry(gltfAssetRegistry)
            .build();

        Log.debug("Subsystems initialized: MeshSystem, TextureSystem, ShaderSystem, CameraSystem");

        sceneManager = new SceneManager(context);
        context.setSceneManager(sceneManager);

        EntitySystem entitySystem = new EntitySystem();
        entitySystem.onInit(context);
        context.setEntitySystem(entitySystem);
    }

    private void mainLoop() {
        Log.debug("Main loop started");
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

            context.getTime().update(rawDelta);
            float delta = context.getTime().getDelta();

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

            InputMap activeInputMap = context.getActiveInputMap();
            if (activeInputMap != null) {
                activeInputMap.frameReset();
            }

            sceneManager.processPendingTransitions();

            glfwSwapBuffers(window.getId());
        }
    }

    private void logActiveSystems(List<GameSystem> systems) {
        StringBuilder sb = new StringBuilder("Active system execution order:\n");
        for (GameSystem system : systems) {
            sb.append("  [").append(system.phase()).append("] ")
              .append(system.getClass().getSimpleName()).append("\n");
        }
        Log.info(sb.toString());
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
