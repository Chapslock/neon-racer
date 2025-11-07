package org.chapzlock.core.system;

import static org.joml.Math.clamp;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.application.System;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.input.keyboard.KeyPressedEvent;
import org.chapzlock.core.input.keyboard.KeyReleasedEvent;
import org.chapzlock.core.input.mouse.MouseMovedEvent;
import org.chapzlock.core.registry.ComponentRegistry;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * Allows to move the camera in a spectator like manner.
 * Useful system for debugging a 3D world.
 */
public class CameraFreeRoamSystem implements System {

    private final ComponentRegistry registry = ComponentRegistry.instance();
    private final EventBus eventBus = EventBus.instance();
    private final List<Subscription> subscriptions = new ArrayList<>();

    private static final float CAMERA_MOUSE_SENSITIVITY = .03f;
    private static final float CAMERA_MOVEMENT_SPEED = 10f;

    // Movement state (driven by key press/release events)
    private boolean movingForward = false;
    private boolean movingBackwards = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingUp = false;
    private boolean movingDown = false;

    // Mouse look state
    private double lastMouseX;
    private double lastMouseY;
    private boolean firstMouse = true;

    @Override
    public void onUpdate(float deltaTime) {
        var view = registry.view(Camera.class);
        if (view.isEmpty()) {
            return;
        }
        Camera camera = view.getFirst().get(Camera.class);
        float velocity = CAMERA_MOVEMENT_SPEED * deltaTime;
        if (movingForward) {
            Vector3f forward = camera.getCameraFront().mul(velocity);
            camera.getPosition().add(forward);
        }
        if (movingBackwards) {
            Vector3f backward = camera.getCameraFront().mul(velocity);
            camera.getPosition().sub(backward);
        }
        if (movingLeft) {
            Vector3f right = camera.getCameraRight().mul(velocity);
            camera.getPosition().sub(right);
        }
        if (movingRight) {
            Vector3f right = camera.getCameraRight().mul(velocity);
            camera.getPosition().add(right);
        }
        if (movingUp) {
            Vector3f up = camera.getCameraUp().mul(velocity);
            camera.getPosition().add(up);
        }
        if (movingDown) {
            Vector3f up = camera.getCameraUp().mul(velocity);
            camera.getPosition().sub(up);
        }
    }

    @Override
    public void onInit() {
        // disable cursor for free-roam
        long window = GLFW.glfwGetCurrentContext();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);

        subscriptions.add(eventBus.subscribe(KeyPressedEvent.class, this::onKeyPressed));
        subscriptions.add(eventBus.subscribe(KeyReleasedEvent.class, this::onKeyReleased));
        subscriptions.add(eventBus.subscribe(MouseMovedEvent.class, this::onMouseMoved));
    }

    @Override
    public void onDestroy() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }

    private boolean onKeyPressed(KeyPressedEvent event) {
        int key = event.key();
        switch (key) {
            case GLFW.GLFW_KEY_W -> movingForward = true;
            case GLFW.GLFW_KEY_S -> movingBackwards = true;
            case GLFW.GLFW_KEY_A -> movingLeft = true;
            case GLFW.GLFW_KEY_D -> movingRight = true;
            case GLFW.GLFW_KEY_SPACE -> movingUp = true;
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> movingDown = true;
        }
        return false;
    }

    private boolean onKeyReleased(KeyReleasedEvent event) {
        int key = event.keyCode();
        switch (key) {
            case GLFW.GLFW_KEY_W -> movingForward = false;
            case GLFW.GLFW_KEY_S -> movingBackwards = false;
            case GLFW.GLFW_KEY_A -> movingLeft = false;
            case GLFW.GLFW_KEY_D -> movingRight = false;
            case GLFW.GLFW_KEY_SPACE -> movingUp = false;
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> movingDown = false;
        }
        return false;
    }

    private boolean onMouseMoved(MouseMovedEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (firstMouse) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            firstMouse = false;
            return false;
        }

        double xOffset = mouseX - lastMouseX;
        double yOffset = lastMouseY - mouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        var view = registry.view(Camera.class);
        if (view.isEmpty()) {
            return false;
        }
        Camera camera = view.getFirst().get(Camera.class);

        float yaw = camera.getYaw() + (float) (xOffset * CAMERA_MOUSE_SENSITIVITY);
        float pitch = camera.getPitch() + (float) (yOffset * CAMERA_MOUSE_SENSITIVITY);

        pitch = clamp(-89f, 89f, pitch);

        camera.setYaw(yaw);
        camera.setPitch(pitch);

        return false;
    }
}
