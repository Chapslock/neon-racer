package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.util.ArrayList;
import java.util.Objects;

import org.chapzlock.core.Layer;
import org.chapzlock.core.window.Window;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Application {
    private static Application INSTANCE;
    private ApplicationSpecification appSpec;
    private Window window;
    private boolean isRunning = false;
    private ArrayList<Layer> layerStack = new ArrayList<>();

    public Application(ApplicationSpecification appSpec) {
        INSTANCE = this;
        this.appSpec = appSpec;

        GLFWErrorCallback.createPrint(System.err).set();

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
        GL.createCapabilities();

        float lastTime = getTime();

        while (this.isRunning) {
            glfwPollEvents();

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
        INSTANCE = null;

        // Terminate GLFW and free the error callback
        glfwTerminate();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }

    public void pushLayer(Layer layer) {
        this.layerStack.add(layer);
    }

    public static Application get() {
        return INSTANCE;
    }

    public static float getTime() {
        return (float) glfwGetTime();
    }
}
