package org.chapzlock.app.component;

import org.chapzlock.core.ecs.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration and runtime state for the third-person camera system.
 *
 * <p>Configuration fields (set once, tuned per game):
 * <ul>
 *   <li>{@code distance} — camera distance from player in world units (default 6)</li>
 *   <li>{@code distanceMin/Max} — zoom clamp range (default 2–20)</li>
 *   <li>{@code zoomSpeed} — scroll wheel sensitivity (default 2)</li>
 *   <li>{@code heightOffset} — vertical offset above player pivot in world units (default 1)</li>
 *   <li>{@code followSmoothing} — lerp factor for position following (higher = snappier, default 8)</li>
 *   <li>{@code rotationSmoothing} — lerp factor for yaw follow in non-orbit mode (default 6)</li>
 *   <li>{@code pitchMin/Max} — pitch clamp in degrees (default -20 to 60)</li>
 *   <li>{@code mouseSensitivity} — orbit mouse sensitivity (default 0.3)</li>
 * </ul>
 *
 * <p>Runtime state (managed by ThirdPersonCameraSystem):
 * <ul>
 *   <li>{@code yaw/pitch} — current camera angles in degrees</li>
 *   <li>{@code orbitingRight/Left} — whether mouse buttons are held</li>
 * </ul>
 */
@Getter
@Setter
public class ThirdPersonCamera implements Component {
    private float distance = 6f;
    private float distanceMin = 2f;
    private float distanceMax = 20f;
    private float zoomSpeed = 2f;
    private float heightOffset = 1.0f;
    private float followSmoothing = 8f;
    private float rotationSmoothing = 6f;
    private float pitchMin = -20f;
    private float pitchMax = 60f;
    private float mouseSensitivity = 0.3f;

    private float yaw;
    private float pitch = 15f;

    private boolean orbitingRight;
    private boolean orbitingLeft;
}
