package org.chapzlock.app.component;

import org.chapzlock.core.ecs.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ThirdPersonCamera implements Component {
    private float distance = 6f;
    private float heightOffset = 2.5f;
    private float followSmoothing = 8f;
    private float rotationSmoothing = 6f;
    private float pitchMin = -20f;
    private float pitchMax = 60f;
    private float mouseSensitivity = 0.3f;

    private float yaw;
    private float pitch = 15f;
    private boolean orbiting;
}
