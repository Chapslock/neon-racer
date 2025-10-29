package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.util.ArrayList;
import java.util.Objects;

import org.chapzlock.core.window.Window;
import org.lwjgl.glfw.GLFWErrorCallback;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Application {
    private static Application instance;
    @Getter
    private ApplicationSpecification appSpec;
    private Window window;
    private boolean isRunning = false;
    private final ArrayList<Layer> layerStack = new ArrayList<>();

    private Application(ApplicationSpecification appSpec) {
        this.appSpec = appSpec;

        GLFWErrorCallback.createPrint(java.lang.System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        if (appSpec.getWindowSpec().getTitle() == null) {
            appSpec.getWindowSpec().setTitle(appSpec.getName());
        }

        this.window = new Window(appSpec.getWindowSpec());
        this.window.create();

    }

    public void run() {
        this.isRunning = true;

        float lastTime = getTime();

        this.layerStack.forEach(Layer::onInit);

        while (this.isRunning) {

            if (this.window.shouldClose()) {
                stop();
                break;
            }

            float currentTime = getTime();
            float timeStep = currentTime - lastTime;
            lastTime = currentTime;

            this.layerStack.forEach(layer -> layer.onUpdate(timeStep));
            this.layerStack.forEach(layer -> layer.onRender(timeStep));

            this.window.update();
        }
    }

    public void stop() {
        this.isRunning = false;
        this.window.destroy();
        instance = null;

        this.layerStack.forEach(Layer::onDestroy);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }

    public void pushLayer(Layer layer) {
        this.layerStack.add(layer);
    }

    public static Application instance() {
        if (instance == null) {
            throw new IllegalStateException("Application instance referenced outside of application!");
        }
        return instance;
    }

    public static Application create(ApplicationSpecification appSpec) {
        instance = new Application(appSpec);
        return instance;
    }

    public static float getTime() {
        return (float) glfwGetTime();
    }
}
