package org.chapzlock.core.input;

import static org.lwjgl.glfw.GLFW.*;

import lombok.Getter;

/**
 * Mouse button identifiers mapped to GLFW button codes.
 *
 * <p>Used in {@link InputMap} to bind mouse buttons to game actions,
 * and in mouse events to identify which button was pressed/released.
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
