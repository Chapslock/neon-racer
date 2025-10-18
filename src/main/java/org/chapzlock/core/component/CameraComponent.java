package org.chapzlock.core.component;


import static org.joml.Math.cos;
import static org.joml.Math.sin;
import static org.joml.Math.toRadians;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CameraComponent implements Component {
    private Vector3f position = new Vector3f(0,0,0);
    /**
     * Camera rotation in degrees (x = pitch, y = yaw, z = roll)
     */
    private Vector3f rotation = new Vector3f(0, -90, 0);

    /**
     * Returns the normalized forward (front) vector of the camera.
     */
    public Vector3f getCameraFront() {
        float yawRad = toRadians(getYaw());
        float pitchRad = toRadians(getPitch());

        Vector3f front = new Vector3f();
        front.x = cos(yawRad) * cos(pitchRad);
        front.y = sin(pitchRad);
        front.z = sin(yawRad) * cos(pitchRad);
        return front.normalize();
    }

    public Vector3f getCameraUp() {
        return new Vector3f(0, 1, 0);
    }

    public Vector3f getCameraRight() {
        return new Vector3f(getCameraFront()).cross(getCameraUp()).normalize();
    }

    public float getYaw() {
        return rotation.y;
    }

    public void setYaw(float value) {
        rotation.y = value;
    }

    public float getPitch() {
        return rotation.x;
    }

    public void setPitch(float value) {
        rotation.x = value;
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), getPosition().add(getCameraFront(), new Vector3f()), getCameraUp());
    }
}
