package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Camera projection parameters defining how the 3D world is projected onto screen.
 *
 * <p>This component is projection-only — it does NOT store position or rotation.
 * The camera entity must also have a {@link Transform} component which provides
 * spatial data (position, rotation). This follows industry convention (Unity, Unreal,
 * Godot) where all spatial state lives on Transform uniformly.
 *
 * <p>The camera's view direction is derived from the Transform's rotation quaternion.
 * Use {@link Transform#lookAt(org.joml.Vector3f)} to point the camera at a target.
 *
 * <p>Requires: {@link Transform} (enforced via ComponentRule).
 */
@Getter
@Setter
public class Camera implements Component {
    private float fieldOfView = 70f;
    private float nearPlane = 0.1f;
    private float farPlane = 1000f;

    public Camera() {}

    public Camera(float fieldOfView, float nearPlane, float farPlane) {
        this.fieldOfView = fieldOfView;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
    }
}
