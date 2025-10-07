package org.chapzlock.core;

import org.chapzlock.core.application.Application;
import org.chapzlock.core.input.Input;
import org.chapzlock.core.math.Vector3f;
import org.lwjgl.glfw.GLFW;

import lombok.Getter;

public class Camera {

    private static final float FIELD_OF_VIEW = 90;
    private static final float NEAR_PLANE = 0.1f;
    private static final float ASPECT_RATIO = (float) Application.get().getAppSpec().getWindowSpec().getWidth() /
        (float) Application.get().getAppSpec().getWindowSpec().getHeight();
    private static final float FAR_PLANE = 1000;


    @Getter
    private Vector3f position = new Vector3f(0,0,0);
    /**
     * Camera rotation in degrees (x = pitch, y = yaw, z = roll)
     */
    @Getter
    private Vector3f rotation = new Vector3f(0,0,0);

    public float getPitch() {
        return rotation.x;
    }

    public float getYaw() {
        return rotation.y;
    }

    public float getRoll() {
        return rotation.z;
    }

    public void onUpdate() {
        float camMoveSpeed = 0.01f;
        if (Input.isKeyPressed(GLFW.GLFW_KEY_W)) {
            position.z-=camMoveSpeed;
        }
        if (Input.isKeyPressed(GLFW.GLFW_KEY_S)) {
            position.z+=camMoveSpeed;
        }
        if (Input.isKeyPressed(GLFW.GLFW_KEY_A)) {
            position.x+=camMoveSpeed;
        }
        if (Input.isKeyPressed(GLFW.GLFW_KEY_D)) {
            position.x-=camMoveSpeed;
        }
    }

}
