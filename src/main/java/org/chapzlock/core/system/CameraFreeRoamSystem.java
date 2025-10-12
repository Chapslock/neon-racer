package org.chapzlock.core.system;

import org.chapzlock.core.component.CameraComponent;
import org.chapzlock.core.component.orchestration.ComponentRegistry;
import org.lwjgl.glfw.GLFW;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CameraFreeRoamSystem implements System {

    private final ComponentRegistry registry;

    private float mouseSensitivity = .05f;
    private float movementSpeed = 3f;

    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;

    @Override
    public void onInit() {

    }

    @Override
    public void onUpdate(float deltaTime) {
        CameraComponent camera = registry.view(CameraComponent.class)
            .getFirst().get(CameraComponent.class);
        long window = GLFW.glfwGetCurrentContext();

        handleMouse(camera, window);
        handleMovement(camera, window, deltaTime);
    }

    private void handleMouse(CameraComponent camera, long window) {
        double[] xPos = new double[1];
        double[] yPos = new double[1];
        GLFW.glfwGetCursorPos(window, xPos, yPos);

        if (firstMouse) {
            lastMouseX = xPos[0];
            lastMouseY = yPos[0];
            firstMouse = false;
        }

        float xOffset = (float) (xPos[0] - lastMouseX) * mouseSensitivity;
        float yOffset = (float) (yPos[0] - lastMouseY) * mouseSensitivity;
        lastMouseX = xPos[0];
        lastMouseY = yPos[0];

        camera.setYaw(camera.getYaw() + xOffset);
        camera.setPitch(camera.getPitch() + yOffset);

        // clamp pitch to prevent flipping
        if (camera.getPitch() > 89.0f) {
            camera.setPitch(89.0f);
        }
        if (camera.getPitch() < -89.0f) {
            camera.setPitch(-89.0f);
        }
    }

    private void handleMovement(CameraComponent camera, long window, float deltaTime) {

    }
}

