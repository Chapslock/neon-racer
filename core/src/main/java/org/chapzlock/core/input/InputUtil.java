package org.chapzlock.core.input;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

import org.lwjgl.glfw.GLFW;

import lombok.experimental.UtilityClass;


/**
 * Utility class for input handling
 */
@UtilityClass
public class InputUtil {


    /**
     * Detects if a certain key is pressed
     * @param key GLFW key to detect. Example {@link GLFW.GLFW_KEY_UP}
     * @return true or false
     */
    public static boolean isKeyPressed(int key) {
        long currentlyActiveWindowHandle = GLFW.glfwGetCurrentContext();
        return glfwGetKey(currentlyActiveWindowHandle, key) == GLFW_PRESS;
    }

}
