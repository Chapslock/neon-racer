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
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.input.mouse.MouseScrolledEvent;
import org.chapzlock.core.window.Window;
import org.joml.Vector3f;

public class ThirdPersonCameraSystem implements GameSystem {

    private ComponentRegistry registry;
    private EngineContext context;
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
    public void onInit(EngineContext ctx) {
        this.context = ctx;
        this.registry = ctx.getRegistry();
        this.window = ctx.getWindow();

        EventBus eventBus = ctx.getEventBus();
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

        var cameraOpt = registry.getSingleton(Camera.class);
        if (cameraOpt.isEmpty()) return;
        Camera camera = cameraOpt.get();

        Vector3f playerPos = getPlayerPosition();
        if (playerPos == null) return;

        InputMap input = context.getActiveInputMap();

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
            float behindYaw = getPlayerYaw() - 90f;
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
