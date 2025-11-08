package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.window.Window;

import lombok.Getter;

/**
 * Main application class — manages the layer stack, windows, and the main loop.
 * Supports multiple windows through WindowManager.
 */
public class Application {
    private static Application instance;
    @Getter
    private final ApplicationSpecification appSpec;
    private final List<Layer> layerStack = new ArrayList<>();
    /**
     * Application window (may be null until attached)
     */
    private Window window;
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
     * Attaches a window to the application and starts the loop
     */
    public void run() {
        if (window == null) {
            Window w = new Window(appSpec.getWindowSpec());
            w.create(0);
            attachWindow(w);
        }

        isRunning = true;
        layerStack.forEach(Layer::onInit);
        float lastTime = getTime();

        while (isRunning) {
            float now = getTime();
            float delta = now - lastTime;
            lastTime = now;

            if (window.shouldClose()) {
                detachWindow();
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
        ApplicationUtil.terminate();
        instance = null;
    }

    /**
     * Attach a window to the application. The window must already be created
     * (Window.create(...)) on the thread that will call run() or otherwise be safe.
     * This method will destroy any previously attached window.
     */
    private void attachWindow(Window w) {
        if (w == null) {
            throw new IllegalArgumentException("window cannot be null");
        }
        if (this.window != null) {
            // safely destroy previous window before replacing
            try {
                this.window.destroy();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        this.window = w;
    }

    /**
     * Gives the time since the intialization of the application
     *
     * @return seconds since the start of the application
     */
    public static float getTime() {
        return (float) glfwGetTime();
    }

    /**
     * Detach (and destroy) the current window.
     */
    private void detachWindow() {
        if (this.window != null) {
            try {
                this.window.destroy();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                this.window = null;
            }
        }
    }

    private void stop() {
        isRunning = false;
    }

    /**
     * @return the application instance
     */
    public static Application instance() {
        if (instance == null) {
            throw new IllegalStateException("Application not yet created!");
        }
        return instance;
    }

    /**
     * Creates a new application and initializes GLFW context
     */
    public static Application create(ApplicationSpecification spec) {
        if (instance != null) {
            throw new IllegalStateException("Application already created!");
        }
        instance = new Application(spec);
        return instance;
    }
}
