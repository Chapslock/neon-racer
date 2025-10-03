package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.util.ArrayList;

import org.chapzlock.core.Layer;
import org.chapzlock.core.window.Window;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

public class Application {
    private static Application APPLICATION;
    private ApplicationSpecification appSpec;
    private Window window;
    private boolean isRunning = false;
    private ArrayList<Layer> layerStack = new ArrayList<>();

    public Application(ApplicationSpecification appSpec) {
        APPLICATION = this;
        this.appSpec = appSpec;

        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();

        if (appSpec.getWindowSpec().getTitle() == null) {
            appSpec.getWindowSpec().setTitle(appSpec.getName());
        }

        this.window = new Window(appSpec.getWindowSpec());
        this.window.create();

    }

    public void run() {
        this.isRunning = true;
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

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
            this.layerStack.forEach(Layer::onRender);

            this.window.update();
        }
    }

    public void stop() {
        this.isRunning = false;
        this.window.destroy();
        APPLICATION = null;

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void pushLayer() {
    }

    public static Application get() {
        return APPLICATION;
    }

    public static float getTime() {
        return (float) glfwGetTime();
    }
}
