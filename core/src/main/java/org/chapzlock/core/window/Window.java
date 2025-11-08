package org.chapzlock.core.window;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.keyboard.KeyPressedEvent;
import org.chapzlock.core.input.keyboard.KeyReleasedEvent;
import org.chapzlock.core.input.mouse.MouseButtonPressedEvent;
import org.chapzlock.core.input.mouse.MouseButtonReleasedEvent;
import org.chapzlock.core.input.mouse.MouseMovedEvent;
import org.chapzlock.core.input.mouse.MouseScrolledEvent;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GLCapabilities;


/**
 * A single GLFW window, wrapping native handles and callbacks.
 * Rendering logic (clearing, swapping, etc.) is handled by the Application.
 */
public class Window {

    private long id = NULL;
    private final WindowSpecification specs;
    private GLCapabilities glCapabilities;
    private final EventBus eventBus = EventBus.instance();

    public Window(WindowSpecification specs) {
        this.specs = specs;
    }

    public long getId() {
        return id;
    }

    public WindowSpecification getSpecification() {
        return specs;
    }

    public GLCapabilities getGLCapabilities() {
        return glCapabilities;
    }

    /**
     * Creates a new GLFW window and its context. Optionally shares context with another window.
     *
     * @param sharedWith another window’s ID or 0 for no sharing
     */
    public void create(long sharedWith) {
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, specs.isResizable() ? GLFW_TRUE : GLFW_FALSE);

        id = glfwCreateWindow(specs.getWidth(), specs.getHeight(), specs.getTitle(), NULL,
            sharedWith == 0 ? NULL : sharedWith);

        if (id == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Center window
        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vid != null) {
            glfwSetWindowPos(id,
                (vid.width() - specs.getWidth()) / 2,
                (vid.height() - specs.getHeight()) / 2
            );
        }

        // Make context current and create GL capabilities
        makeContextCurrent();
        glCapabilities = createCapabilities();

        // Enable vsync
        glfwSwapInterval(specs.isVSyncEnabled() ? GLFW_TRUE : GLFW_FALSE);

        // Register input/window callbacks
        initializeCallbacks();

        glfwShowWindow(id);
    }

    public void makeContextCurrent() {
        glfwMakeContextCurrent(id);
    }

    /**
     * Simple input + resize + close callbacks
     */
    private void initializeCallbacks() {
        glfwSetKeyCallback(id, (long window, int key, int scancode, int action, int mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }
            if (action == GLFW_PRESS) {
                eventBus.publish(new KeyPressedEvent(key, mods));
            } else if (action == GLFW_RELEASE) {
                eventBus.publish(new KeyReleasedEvent(key, mods));
            }
        });

        glfwSetCursorPosCallback(id, (window, xpos, ypos) -> eventBus.publish(new MouseMovedEvent(xpos, ypos)));

        glfwSetMouseButtonCallback(id, (window, button, action, mods) -> {
            double[] x = new double[1];
            double[] y = new double[1];
            glfwGetCursorPos(window, x, y);
            if (action == GLFW_PRESS) {
                eventBus.publish(new MouseButtonPressedEvent(button, x[0], y[0]));
            } else if (action == GLFW_RELEASE) {
                eventBus.publish(new MouseButtonReleasedEvent(button, x[0], y[0]));
            }
        });

        glfwSetScrollCallback(id, (window, xOffset, yOffset) -> eventBus.publish(new MouseScrolledEvent(xOffset, yOffset)));

        glfwSetFramebufferSizeCallback(id, (window, width, height) -> {
            specs.setHeight(height);
            specs.setWidth(width);
            eventBus.publish(new WindowResizeEvent(window, width, height));
        });
        glfwSetWindowCloseCallback(id, window -> eventBus.publish(new WindowCloseEvent(window)));

    }

    public void destroy() {
        if (id != NULL) {
            glfwDestroyWindow(id);
            id = NULL;
        }
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(id);
    }
}
