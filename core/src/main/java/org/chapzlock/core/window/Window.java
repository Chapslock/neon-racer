package org.chapzlock.core.window;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
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
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.keyboard.KeyPressedEvent;
import org.chapzlock.core.input.keyboard.KeyReleasedEvent;
import org.chapzlock.core.input.mouse.MouseButtonPressedEvent;
import org.chapzlock.core.input.mouse.MouseButtonReleasedEvent;
import org.chapzlock.core.input.mouse.MouseMovedEvent;
import org.chapzlock.core.input.mouse.MouseScrolledEvent;
import org.chapzlock.core.logging.Log;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;


/**
 * GLFW window wrapper managing the application's display surface and input callbacks.
 *
 * <p><strong>Lifecycle:</strong>
 * <ol>
 *   <li>{@code new Window(spec, eventBus)} — stores configuration, no native resources yet</li>
 *   <li>{@link #create(long)} — creates the GLFW window, sets up OpenGL context, registers
 *       input callbacks that publish events to the EventBus, and makes the window visible</li>
 *   <li>Main loop calls {@code glfwPollEvents()} externally — callbacks fire, events publish</li>
 *   <li>{@link #destroy()} — destroys the native window and frees GLFW resources</li>
 * </ol>
 *
 * <p><strong>Input callbacks:</strong> Key, mouse button, mouse move, and scroll events are
 * published to the {@link org.chapzlock.core.event.EventBus} as typed event records
 * (KeyPressedEvent, MouseMovedEvent, etc.). Systems subscribe to these events rather than
 * polling GLFW directly.
 *
 * <p><strong>Cursor modes:</strong> {@link #setCursorLocked(boolean)} toggles between normal
 * cursor (visible, confined to window) and disabled cursor (hidden, infinite movement for
 * mouse-look). Used by camera systems during orbit.
 */
public class Window {

    private long id = NULL;
    private final WindowSpecification specs;
    private GLCapabilities glCapabilities;
    private final EventBus eventBus;

    public Window(WindowSpecification specs, EventBus eventBus) {
        this.specs = specs;
        this.eventBus = eventBus;
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
     * @param sharedWith another window's ID or 0 for no sharing
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
        Log.debug("GLFW window shown: \"" + specs.getTitle() + "\"");
    }

    public void makeContextCurrent() {
        glfwMakeContextCurrent(id);
    }

    /**
     * Simple input + resize + close callbacks
     */
    private void initializeCallbacks() {
        glfwSetKeyCallback(id, (long window, int key, int scancode, int action, int mods) -> {
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
            GL11.glViewport(0, 0, width, height);
            eventBus.publish(new WindowResizeEvent(window, width, height));
        });
        glfwSetWindowCloseCallback(id, window -> eventBus.publish(new WindowCloseEvent(window)));

    }

    public void setCursorLocked(boolean locked) {
        if (id == NULL) return;
        glfwSetInputMode(id, GLFW_CURSOR, locked ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
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
