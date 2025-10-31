package org.chapzlock.core.system;

import static org.joml.Math.clamp;

import org.chapzlock.core.application.System;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.registry.ComponentRegistry;
import org.lwjgl.glfw.GLFW;

/**
 * Allows to move the camera in a spectator like manner.
 * Useful system for debugging a 3D world.
 */
public class CameraFreeRoamSystem implements System {

    private final ComponentRegistry registry = ComponentRegistry.instance();

    private static final float CAMERA_MOUSE_SENSITIVITY = .03f;
    private static final float CAMERA_MOVEMENT_SPEED = 10f;

    private double lastMouseX;
    private double lastMouseY;
    private boolean firstMouse = true;

    @Override
    public void onInit() {
        long window = GLFW.glfwGetCurrentContext();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    @Override
    public void onUpdate(float deltaTime) {
        Camera camera = registry.view(Camera.class)
            .getFirst().get(Camera.class);
        long window = GLFW.glfwGetCurrentContext();

        handleMouseInput(window, camera);
        handleKeyboardInput(window, camera, deltaTime);
    }

    private void handleMouseInput(long window, Camera camera) {
        double mouseX;
        double mouseY;
        try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
            var xPos = stack.mallocDouble(1);
            var yPos = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(window, xPos, yPos);
            mouseX = xPos.get(0);
            mouseY = yPos.get(0);
        }

        if (firstMouse) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            firstMouse = false;
        }

        double xOffset = mouseX - lastMouseX;
        double yOffset = lastMouseY - mouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        float yaw = camera.getYaw() + (float) (xOffset * CAMERA_MOUSE_SENSITIVITY);
        float pitch = camera.getPitch() + (float) (yOffset * CAMERA_MOUSE_SENSITIVITY);

        pitch = clamp(-89f, 89f, pitch);

        camera.setYaw(yaw);
        camera.setPitch(pitch);
    }

    private void handleKeyboardInput(long window, Camera camera, float deltaTime) {
        float velocity = CAMERA_MOVEMENT_SPEED * deltaTime;

        // Move forward/back
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            camera.getPosition().add(camera.getCameraFront().mul(velocity));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            camera.getPosition().sub(camera.getCameraFront().mul(velocity));
        }

        // Strafe left/right
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            camera.getPosition().sub(camera.getCameraRight().mul(velocity));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            camera.getPosition().add(camera.getCameraRight().mul(velocity));
        }

        // Move up/down (vertical movement)
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            camera.getPosition().add(camera.getCameraUp().mul(velocity));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            camera.getPosition().sub(camera.getCameraUp().mul(velocity));
        }
    }
}

