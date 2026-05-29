package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

import java.util.List;

import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.render.CameraSystem;
import org.chapzlock.core.render.EntityMaterialRenderer;
import org.chapzlock.core.render.MaterialRendererRegistry;
import org.chapzlock.core.render.MaterialSystem;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TerrainMaterialRenderer;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.render.UiMaterialRenderer;
import org.chapzlock.core.scene.MaterialType;
import org.chapzlock.core.scene.SceneManager;
import org.chapzlock.core.window.Window;
import org.chapzlock.core.window.WindowSpecification;

import lombok.Getter;

public class Application {
    @Getter
    private final ApplicationSpecification appSpec;
    private EngineContext context;
    private boolean isRunning = false;

    private Application(ApplicationSpecification spec) {
        this.appSpec = spec;
        ApplicationUtil.init();
    }

    public void run() {
        EventBus eventBus = new EventBus();
        ComponentRegistry registry = new ComponentRegistry(eventBus);

        Window window = new Window(appSpec.getWindowSpec(), eventBus);
        window.create(0);

        ShaderSystem shaderSystem = new ShaderSystem();
        TextureSystem textureSystem = new TextureSystem();
        MeshSystem meshSystem = new MeshSystem();
        MaterialRendererRegistry materialRendererRegistry = new MaterialRendererRegistry(shaderSystem);
        MaterialSystem materialSystem = new MaterialSystem(materialRendererRegistry);
        CameraSystem cameraSystem = new CameraSystem(appSpec.getWindowSpec());

        context = new EngineContext(
            registry, eventBus, meshSystem, textureSystem,
            shaderSystem, materialSystem, materialRendererRegistry, cameraSystem, window
        );

        registerDefaultMaterialRenderers(shaderSystem, textureSystem, cameraSystem,
            materialRendererRegistry, appSpec.getWindowSpec());

        SceneManager sceneManager = new SceneManager(context);
        context.setSceneManager(sceneManager);

        List<GameSystem> engineSystems = appSpec.getEngineSystems();
        sceneManager.registerEngineSystems(engineSystems);

        engineSystems.forEach(system -> system.onInit(context));
        logExecutionOrder(engineSystems);

        if (appSpec.getInitialScene() != null) {
            sceneManager.loadImmediate(appSpec.getInitialScene());
        }

        isRunning = true;
        float lastTime = getTime();

        while (isRunning) {
            float now = getTime();
            float delta = now - lastTime;
            lastTime = now;

            if (window.shouldClose()) {
                stop();
                break;
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

        sceneManager.unloadAll();
        engineSystems.forEach(GameSystem::onDestroy);
        context.dispose();
        window.destroy();
        ApplicationUtil.terminate();
    }

    private void registerDefaultMaterialRenderers(
            ShaderSystem shaderSystem, TextureSystem textureSystem,
            CameraSystem cameraSystem, MaterialRendererRegistry registry,
            WindowSpecification windowSpec) {
        registry.registerDefault(MaterialType.ENTITY,
            new EntityMaterialRenderer(shaderSystem, textureSystem, cameraSystem));
        registry.registerDefault(MaterialType.TERRAIN,
            new TerrainMaterialRenderer(shaderSystem, textureSystem, cameraSystem));
        registry.registerDefault(MaterialType.UI,
            new UiMaterialRenderer(shaderSystem, textureSystem, cameraSystem, windowSpec));
    }

    private void logExecutionOrder(List<GameSystem> systems) {
        StringBuilder sb = new StringBuilder("Engine system execution order:\n");
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
