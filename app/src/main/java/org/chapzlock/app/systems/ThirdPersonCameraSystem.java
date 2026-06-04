package org.chapzlock.app.systems;

import java.util.List;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.component.ThirdPersonCamera;
import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.input.mouse.MouseScrolledEvent;
import org.chapzlock.core.window.Window;
import org.joml.Vector3f;

/**
 * Third-person camera that follows the player using spherical coordinates.
 *
 * <p>Camera position is computed from three parameters stored in {@link ThirdPersonCamera}:
 * distance, pitch (vertical angle), and yaw (horizontal angle). These map to Cartesian
 * coordinates via: x = dist * cos(pitch) * sin(yaw), y = dist * sin(pitch),
 * z = dist * cos(pitch) * cos(yaw).
 *
 * <p><strong>Modes:</strong>
 * <ul>
 *   <li><em>Follow mode</em> (no mouse button held) — camera yaw lerps toward player's
 *       facing direction. Smooth rotation behind the player.</li>
 *   <li><em>Orbit mode</em> (left or right mouse button held) — mouse movement directly
 *       controls yaw and pitch. Cursor is locked during orbit.</li>
 * </ul>
 *
 * <p>Scroll wheel adjusts distance (zoom). Position is smoothed via lerp with
 * delta-time scaling for frame-rate independence. Pitch is clamped to prevent
 * gimbal lock at the poles.
 *
 * <p>Runs after {@link CharacterRotationSystem} to read the latest player facing yaw.
 */
public class ThirdPersonCameraSystem implements GameSystem {

    private ComponentRegistry registry;
    private Engine engine;
    private Window window;
    private float pendingScrollDelta;
    private Subscription scrollSubscription;

    @Override
    public SystemPhase phase() { return SystemPhase.LOGIC; }

    @Override
    public List<Class<? extends GameSystem>> runAfter() {
        return List.of(CharacterRotationSystem.class);
    }

    @Override
    public void onInit(Engine engine) {
        this.engine = engine;
        this.registry = engine.getRegistry();
        this.window = engine.getWindow();

        EventBus eventBus = engine.getEventBus();
        scrollSubscription = eventBus.subscribe(MouseScrolledEvent.class, event -> {
            pendingScrollDelta += (float) event.yOffset();
            return false;
        });
    }

    @Override
    public void onDestroy() {
        if (scrollSubscription != null) scrollSubscription.unsubscribe();
    }

    @Override
    public void onUpdate(float deltaTime) {
        var camOpt = registry.getSingleton(ThirdPersonCamera.class);
        if (camOpt.isEmpty()) return;
        ThirdPersonCamera tpc = camOpt.get();

        final Transform[] camTransformHolder = {null};
        registry.forEach(ThirdPersonCamera.class, Transform.class, (entityId, tpcComp, transform) -> {
            if (camTransformHolder[0] == null) {
                camTransformHolder[0] = transform;
            }
        });
        Transform camTransform = camTransformHolder[0];
        if (camTransform == null) return;

        Vector3f playerPos = getPlayerPosition();
        if (playerPos == null) return;

        InputMap input = engine.getActiveInputMap();

        // Handle orbit mode transitions
        boolean rightHeld = input != null && input.isPressed("cameraOrbit");
        boolean leftHeld = input != null && input.isPressed("cameraLook");

        if (rightHeld && !tpc.isOrbitingRight()) {
            tpc.setOrbitingRight(true);
            if (window != null) window.setCursorLocked(true);
        } else if (!rightHeld && tpc.isOrbitingRight()) {
            tpc.setOrbitingRight(false);
            if (!leftHeld && window != null) window.setCursorLocked(false);
        }

        if (leftHeld && !tpc.isOrbitingLeft()) {
            tpc.setOrbitingLeft(true);
            if (window != null) window.setCursorLocked(true);
        } else if (!leftHeld && tpc.isOrbitingLeft()) {
            tpc.setOrbitingLeft(false);
            if (!rightHeld && window != null) window.setCursorLocked(false);
        }

        // Mouse orbit (either button)
        if ((tpc.isOrbitingRight() || tpc.isOrbitingLeft()) && input != null) {
            float dx = input.getMouseAxis("lookX");
            float dy = input.getMouseAxis("lookY");
            tpc.setYaw(tpc.getYaw() - dx * tpc.getMouseSensitivity());
            tpc.setPitch(tpc.getPitch() + dy * tpc.getMouseSensitivity());
            tpc.setPitch(Math.max(tpc.getPitchMin(), Math.min(tpc.getPitchMax(), tpc.getPitch())));
        } else {
            // Follow mode: lerp camera behind player
            float behindYaw = getPlayerFacingYaw() - 90f;
            float yawDiff = behindYaw - tpc.getYaw();
            while (yawDiff > 180f) yawDiff -= 360f;
            while (yawDiff < -180f) yawDiff += 360f;
            tpc.setYaw(tpc.getYaw() + yawDiff * Math.min(1f, tpc.getRotationSmoothing() * deltaTime));
        }

        // Scroll zoom
        if (pendingScrollDelta != 0f) {
            float newDist = tpc.getDistance() - pendingScrollDelta * tpc.getZoomSpeed();
            tpc.setDistance(Math.max(tpc.getDistanceMin(), Math.min(tpc.getDistanceMax(), newDist)));
            pendingScrollDelta = 0f;
        }

        // Compute camera position from spherical coordinates
        float yawRad = (float) Math.toRadians(tpc.getYaw());
        float pitchRad = (float) Math.toRadians(tpc.getPitch());

        float dist = tpc.getDistance();
        float horizontalDist = (float) (dist * Math.cos(pitchRad));
        float verticalDist = (float) (dist * Math.sin(pitchRad));

        float camX = playerPos.x + (float) (horizontalDist * Math.sin(yawRad));
        float camY = playerPos.y + tpc.getHeightOffset() + verticalDist;
        float camZ = playerPos.z + (float) (horizontalDist * Math.cos(yawRad));

        // Smooth position (write to Transform)
        Vector3f currentPos = new Vector3f();
        camTransform.getPosition(currentPos);
        float t = Math.min(1f, tpc.getFollowSmoothing() * deltaTime);
        currentPos.lerp(new Vector3f(camX, camY, camZ), t);
        camTransform.setPosition(currentPos);

        // Look at player (write rotation to Transform)
        Vector3f lookTarget = new Vector3f(playerPos.x, playerPos.y + tpc.getHeightOffset(), playerPos.z);
        camTransform.lookAt(lookTarget);
    }

    private Vector3f getPlayerPosition() {
        int playerEntity = registry.getEntityByName("player");
        if (playerEntity < 0) return null;
        Transform transform = registry.getComponent(playerEntity, Transform.class);
        if (transform == null) return null;
        Vector3f pos = new Vector3f();
        transform.getPosition(pos);
        return pos;
    }

    private float getPlayerFacingYaw() {
        int playerEntity = registry.getEntityByName("player");
        if (playerEntity < 0) return 0f;
        CharacterState state = registry.getComponent(playerEntity, CharacterState.class);
        return state != null ? state.getFacingYaw() : 0f;
    }
}
