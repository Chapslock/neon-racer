package org.chapzlock.core.application;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;

import org.lwjgl.glfw.GLFWErrorCallback;

import lombok.experimental.UtilityClass;


/**
 * Handles global GLFW initialization and termination.
 * Ensures GLFW is initialized exactly once per process.
 */
@UtilityClass
public final class ApplicationUtil {

    private static boolean initialized = false;
    private static GLFWErrorCallback errorCallback;

    /**
     * Initializes GLFW and installs an error callback.
     * Safe to call multiple times — will only initialize once.
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }

        errorCallback = GLFWErrorCallback.createPrint(java.lang.System.err);
        glfwSetErrorCallback(errorCallback);

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        initialized = true;
    }

    /**
     * Terminates GLFW and frees resources.
     * Safe to call multiple times.
     */
    public static synchronized void terminate() {
        if (!initialized) {
            return;
        }

        glfwTerminate();

        if (errorCallback != null) {
            errorCallback.free();
            errorCallback = null;
        }

        initialized = false;
    }

    /**
     * Returns whether GLFW has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    public static void clearAndPrepareWindowState() {
        glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
        glClearColor(0f, 0f, 0f, 0f);
    }
}
