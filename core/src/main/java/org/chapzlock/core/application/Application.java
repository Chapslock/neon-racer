package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

import java.util.List;

import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.render.CameraSystem;
import org.chapzlock.core.render.EntityMaterialRenderer;
import org.chapzlock.core.render.MaterialSystem;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TerrainMaterialRenderer;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.render.UiMaterialRenderer;
import org.chapzlock.core.scene.MaterialType;
import org.chapzlock.core.scene.SceneManager;
import org.chapzlock.core.tween.TweenManager;
import org.chapzlock.core.window.Window;
import org.chapzlock.core.window.WindowSpecification;

import lombok.Getter;

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
        MaterialSystem materialSystem = new MaterialSystem(shaderSystem);
        CameraSystem cameraSystem = new CameraSystem(appSpec.getWindowSpec());
        Time time = new Time();
        TweenManager tweenManager = new TweenManager();

        context = EngineContext.builder()
            .registry(registry)
            .eventBus(eventBus)
            .meshSystem(meshSystem)
            .textureSystem(textureSystem)
            .shaderSystem(shaderSystem)
            .materialSystem(materialSystem)
            .cameraSystem(cameraSystem)
            .window(window)
            .time(time)
            .tweenManager(tweenManager)
            .build();

        WindowSpecification windowSpec = appSpec.getWindowSpec();
        materialSystem.registerDefault(MaterialType.ENTITY,
            new EntityMaterialRenderer(shaderSystem, textureSystem, cameraSystem));
        materialSystem.registerDefault(MaterialType.TERRAIN,
            new TerrainMaterialRenderer(shaderSystem, textureSystem, cameraSystem));
        materialSystem.registerDefault(MaterialType.UI,
            new UiMaterialRenderer(shaderSystem, textureSystem, cameraSystem, windowSpec));

        Log.debug("Subsystems initialized: MeshSystem, TextureSystem, ShaderSystem, MaterialSystem, CameraSystem");

        sceneManager = new SceneManager(context);
        context.setSceneManager(sceneManager);
    }

    private void mainLoop() {
        Log.debug("Main loop started");
        isRunning = true;
        float lastTime = getTime();

        while (isRunning) {
            float now = getTime();
            float rawDelta = now - lastTime;
            lastTime = now;

            if (window.shouldClose()) {
                stop();
                break;
            }

            context.getTime().update(rawDelta);
            float delta = context.getTime().getDelta();

            InputMap activeInputMap = context.getActiveInputMap();
            if (activeInputMap != null) {
                activeInputMap.frameReset();
            }

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

            sceneManager.processPendingTransitions();

            glfwSwapBuffers(window.getId());
            glfwPollEvents();
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

    public static float getTime() {
        return (float) glfwGetTime();
    }

    private void stop() {
        isRunning = false;
    }

    public static Application create(ApplicationSpecification spec) {
        return new Application(spec);
    }
}
