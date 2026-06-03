package org.chapzlock.app.systems;

import java.util.List;

import org.chapzlock.app.component.ThirdPersonCamera;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.window.Window;
import org.joml.Vector3f;

/**
 * Third-person camera system with two modes: follow-behind (default) and orbit (right-mouse held).
 *
 * <p>In follow mode the camera smoothly tracks behind the player, lerping its yaw toward the
 * player's facing direction. In orbit mode (activated by holding the "cameraOrbit" action),
 * the player can freely rotate the camera around the character using mouse movement.
 *
 * <p>Runs after {@link CharacterRotationSystem} so that the player's rotation is up-to-date
 * before the camera computes its position.
 */
public class ThirdPersonCameraSystem implements GameSystem {

    private ComponentRegistry registry;
    private EngineContext context;
    private Window window;

    @Override
    public SystemPhase phase() {
        return SystemPhase.LOGIC;
    }

    @Override
    public List<Class<? extends GameSystem>> runAfter() {
        return List.of(CharacterRotationSystem.class);
    }

    @Override
    public void onInit(EngineContext ctx) {
        this.context = ctx;
        this.registry = ctx.getRegistry();
        this.window = ctx.getWindow();
    }

    @Override
    public void onUpdate(float deltaTime) {
        // Get ThirdPersonCamera component (singleton on camera entity)
        var camOpt = registry.getSingleton(ThirdPersonCamera.class);
        if (camOpt.isEmpty()) return;
        ThirdPersonCamera tpc = camOpt.get();

        // Get Camera component (singleton on same entity)
        var cameraOpt = registry.getSingleton(Camera.class);
        if (cameraOpt.isEmpty()) return;
        Camera camera = cameraOpt.get();

        // Get player position
        Vector3f playerPos = getPlayerPosition();
        if (playerPos == null) return;

        InputMap input = context.getActiveInputMap();

        // Handle orbit mode transitions
        boolean orbitPressed = input != null && input.isPressed("cameraOrbit");
        if (orbitPressed && !tpc.isOrbiting()) {
            tpc.setOrbiting(true);
            if (window != null) window.setCursorLocked(true);
        } else if (!orbitPressed && tpc.isOrbiting()) {
            tpc.setOrbiting(false);
            if (window != null) window.setCursorLocked(false);
        }

        // Update yaw/pitch
        if (tpc.isOrbiting() && input != null) {
            float dx = input.getMouseAxis("lookX");
            float dy = input.getMouseAxis("lookY");
            tpc.setYaw(tpc.getYaw() + dx * tpc.getMouseSensitivity());
            tpc.setPitch(tpc.getPitch() - dy * tpc.getMouseSensitivity());
            tpc.setPitch(Math.max(tpc.getPitchMin(), Math.min(tpc.getPitchMax(), tpc.getPitch())));
        } else {
            // Follow mode: lerp yaw toward player facing direction
            float playerYaw = getPlayerYaw();
            float yawDiff = playerYaw - tpc.getYaw();
            // Normalize to [-180, 180]
            while (yawDiff > 180f) yawDiff -= 360f;
            while (yawDiff < -180f) yawDiff += 360f;
            tpc.setYaw(tpc.getYaw() + yawDiff * Math.min(1f, tpc.getRotationSmoothing() * deltaTime));
        }

        // Compute camera position from orbit parameters
        float yawRad = (float) Math.toRadians(tpc.getYaw());
        float pitchRad = (float) Math.toRadians(tpc.getPitch());

        float dist = tpc.getDistance();
        float horizontalDist = (float) (dist * Math.cos(pitchRad));
        float verticalDist = (float) (dist * Math.sin(pitchRad));

        float camX = playerPos.x + (float) (horizontalDist * Math.sin(yawRad));
        float camY = playerPos.y + tpc.getHeightOffset() + verticalDist;
        float camZ = playerPos.z + (float) (horizontalDist * Math.cos(yawRad));

        // Smooth position
        Vector3f currentPos = camera.getPosition();
        float t = Math.min(1f, tpc.getFollowSmoothing() * deltaTime);
        currentPos.lerp(new Vector3f(camX, camY, camZ), t);

        // Look at player
        Vector3f lookTarget = new Vector3f(playerPos.x, playerPos.y + tpc.getHeightOffset(), playerPos.z);
        camera.lookAt(lookTarget);
    }

    private Vector3f getPlayerPosition() {
        final Vector3f[] result = {null};
        registry.forEach(PlayerTag.class, Transform.class, (entityId, tag, transform) -> {
            result[0] = new Vector3f();
            transform.getPosition(result[0]);
        });
        return result[0];
    }

    private float getPlayerYaw() {
        final float[] result = {0f};
        registry.forEach(PlayerTag.class, Transform.class, (entityId, tag, transform) -> {
            Vector3f rot = new Vector3f();
            transform.getRotationEuler(rot);
            result[0] = rot.y;
        });
        return result[0];
    }
}
