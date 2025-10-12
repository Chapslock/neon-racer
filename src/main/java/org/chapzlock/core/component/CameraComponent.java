package org.chapzlock.core.component;


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

    private static final Vector3f WORLD_UP = new Vector3f(0, 1, 0);

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

    /**
     * Returns the normalized forward (front) vector of the camera.
     */
    public Vector3f getForwardVector() {
        float yawRad = (float) Math.toRadians(getYaw());
        float pitchRad = (float) Math.toRadians(getPitch());

        Vector3f front = new Vector3f();
        front.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        front.y = (float) (Math.sin(pitchRad));
        front.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        return front.normalize();
    }

    /**
     * Returns the normalized right vector of the camera.
     */
    public Vector3f getRightVector() {
        return new Vector3f(WORLD_UP.cross(getForwardVector(), new Vector3f()).normalize());
    }

    /**
     * Returns the normalized up vector of the camera.
     */
    public Vector3f getUpVector() {
        return new Vector3f(getForwardVector()).cross(getRightVector(), new Vector3f()).normalize();
    }
}
