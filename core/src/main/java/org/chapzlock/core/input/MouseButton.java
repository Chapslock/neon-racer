package org.chapzlock.core.input;

import static org.lwjgl.glfw.GLFW.*;

import lombok.Getter;

/**
 * Mouse button enum wrapping GLFW mouse button codes.
 *
 * <p>Used with {@link InputMapBuilder#action(String, MouseButton)} to bind mouse buttons
 * as input actions alongside keyboard keys.
 */
public enum MouseButton {
    LEFT(GLFW_MOUSE_BUTTON_LEFT),
    RIGHT(GLFW_MOUSE_BUTTON_RIGHT),
    MIDDLE(GLFW_MOUSE_BUTTON_MIDDLE);

    @Getter private final int glfwCode;

    MouseButton(int glfwCode) {
        this.glfwCode = glfwCode;
    }
}
