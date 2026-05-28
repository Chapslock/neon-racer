package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.render.MaterialRendererRegistry;
import org.chapzlock.core.render.MaterialSystem;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.window.Window;

import lombok.Getter;

public class Application {
    @Getter
    private final ApplicationSpecification appSpec;
    private final List<Layer> layerStack = new ArrayList<>();
    private EngineContext context;
    private boolean isRunning = false;

    private Application(ApplicationSpecification spec) {
        this.appSpec = spec;
        ApplicationUtil.init();
    }

    public void pushLayer(Layer layer) {
        layerStack.add(layer);
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

        context = new EngineContext(
            registry, eventBus, meshSystem, textureSystem,
            shaderSystem, materialSystem, materialRendererRegistry, window
        );

        List<GameSystem> sortedSystems = collectAndSortSystems();
        logExecutionOrder(sortedSystems);

        isRunning = true;
        sortedSystems.forEach(system -> system.onInit(context));
        layerStack.forEach(layer -> layer.onInit(context));

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

            for (GameSystem system : sortedSystems) {
                system.onUpdate(delta);
            }
            for (GameSystem system : sortedSystems) {
                system.onRender(delta);
            }

            glfwSwapBuffers(window.getId());
            glfwPollEvents();
        }

        sortedSystems.forEach(GameSystem::onDestroy);
        layerStack.forEach(Layer::onDestroy);
        context.dispose();
        window.destroy();
        ApplicationUtil.terminate();
    }

    private List<GameSystem> collectAndSortSystems() {
        List<GameSystem> all = new ArrayList<>();
        for (Layer layer : layerStack) {
            all.addAll(layer.systems());
        }
        all.sort((a, b) -> Integer.compare(a.phase().ordinal(), b.phase().ordinal()));
        return all;
    }

    private void logExecutionOrder(List<GameSystem> systems) {
        StringBuilder sb = new StringBuilder("System execution order:\n");
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
