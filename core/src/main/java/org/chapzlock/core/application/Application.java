package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.render.MaterialRendererRegistry;
import org.chapzlock.core.render.MaterialSystem;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.window.Window;

import lombok.Getter;

/**
 * Main application class — manages the layer stack, windows, and the main loop.
 */
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

    /**
     * Main loop of the application
     */
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

        isRunning = true;
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

            layerStack.forEach(layer -> layer.onUpdate(delta));
            layerStack.forEach(layer -> layer.onRender(delta));

            glfwSwapBuffers(window.getId());
            glfwPollEvents();
        }

        layerStack.forEach(Layer::onDestroy);
        context.dispose();
        window.destroy();
        ApplicationUtil.terminate();
    }

    /**
     * Gives the time since the initialization of the application
     *
     * @return seconds since the start of the application
     */
    public static float getTime() {
        return (float) glfwGetTime();
    }

    private void stop() {
        isRunning = false;
    }

    /**
     * Creates a new application instance
     */
    public static Application create(ApplicationSpecification spec) {
        return new Application(spec);
    }
}
