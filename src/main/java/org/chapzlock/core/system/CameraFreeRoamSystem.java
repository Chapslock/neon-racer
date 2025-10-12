package org.chapzlock.core.system;

import org.chapzlock.core.component.CameraComponent;
import org.chapzlock.core.component.orchestration.ComponentRegistry;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CameraFreeRoamSystem implements System {

    private final ComponentRegistry registry;

    private float mouseSensitivity = .01f;
    private float movementSpeed = 3f;

    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;

    @Override
    public void onInit() {
        long window = GLFW.glfwGetCurrentContext();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    @Override
    public void onUpdate(float deltaTime) {
        CameraComponent camera = registry.view(CameraComponent.class)
            .getFirst().get(CameraComponent.class);
        long window = GLFW.glfwGetCurrentContext();

        handleMouseInput(window, camera);
        handleKeyboardInput(window, camera, deltaTime);
    }

    private void handleMouseInput(long window, CameraComponent camera) {
        double mouseX, mouseY;
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
        double yOffset = lastMouseY - mouseY; // invert Y
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        float yaw = camera.getYaw() - (float) (xOffset * mouseSensitivity);
        float pitch = camera.getPitch() + (float) (yOffset * mouseSensitivity);

        // Clamp pitch to avoid flipping
        pitch = Math.max(-89.9f, Math.min(89.9f, pitch));

        camera.setYaw(yaw);
        camera.setPitch(pitch);
    }

    private void handleKeyboardInput(long window, CameraComponent camera, float deltaTime) {
        float velocity = movementSpeed * deltaTime;

        // Flatten forward and right vectors for XZ movement
        Vector3f forward = new Vector3f(camera.getForwardVector());
        forward.y = 0;
        forward.normalize();

        Vector3f right = new Vector3f(camera.getRightVector());
        right.y = 0;
        right.normalize();

        // Move forward/back
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            camera.getPosition().add(new Vector3f(forward).mul(velocity));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            camera.getPosition().sub(new Vector3f(forward).mul(velocity));
        }

        // Strafe left/right
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            camera.getPosition().sub(new Vector3f(right).mul(velocity));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            camera.getPosition().add(new Vector3f(right).mul(velocity));
        }

        // Move up/down (vertical movement)
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            camera.getPosition().add(new Vector3f(0, 1, 0).mul(velocity));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            camera.getPosition().sub(new Vector3f(0, 1, 0).mul(velocity));
        }
    }
}

