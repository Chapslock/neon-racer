package org.chapzlock.app.systems;

import java.util.List;

import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.System;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.registry.ComponentRegistry;
import org.joml.Vector3f;


public class PlayerCameraFollowSystem implements System {

    private final ComponentRegistry registry = ComponentRegistry.instance();

    private final float followDistance = 12f;   // behind the car
    private final float height = 4.0f;          // above the car
    private final float lateralOffset = 0.0f;   // side offset (for chase-cam feel)
    private final float positionDamping = 6f;   // larger = snappier
    private final float rotationDamping = 8f;   // larger = snappier

    // persistent state for smoothing between frames
    private final Vector3f camVelocity = new Vector3f();    // for position smooth
    private final Vector3f lookVelocity = new Vector3f();   // for look-target smooth
    private boolean initialized = false;

    @Override
    public void onUpdate(float deltaTime) {
        List<EntityView> cameras = registry.view(Camera.class);
        List<EntityView> players = registry.view(PlayerTag.class, Transform.class);

        if (cameras.isEmpty() || players.isEmpty()) {
            return;
        }

        EntityView camEntity = cameras.get(0);
        EntityView playerEntity = players.get(0);

        Camera camT = camEntity.get(Camera.class);
        Transform playerT = playerEntity.get(Transform.class);

        Vector3f playerPos = playerT.getPosition();
        Vector3f playerForward = new Vector3f(playerT.getForwardVector()).normalize();

        // compute lateral offset properly: along the car's right vector
        Vector3f up = new Vector3f(0f, 1f, 0f);
        Vector3f right = new Vector3f();
        playerForward.cross(up, right);
        if (right.lengthSquared() < 1e-6f) {
            // fallback if forward is exactly up/down
            right.set(1f, 0f, 0f);
        } else {
            right.normalize();
        }
        Vector3f lateral = new Vector3f(right).mul(lateralOffset);

        // desired camera world position (behind + height + lateral)
        Vector3f desired = new Vector3f(
            playerPos.x - playerForward.x * followDistance,
            playerPos.y + height,
            playerPos.z - playerForward.z * followDistance
        );
        desired.add(lateral);

        // initialize camera velocity on first frame to avoid a big jump
        if (!initialized) {
            camT.setPosition(desired);
            // initialize look target exactly to avoid snapping
            Vector3f initialLook = new Vector3f(playerPos.x, playerPos.y + 1.2f, playerPos.z);
            camT.lookAt(initialLook);
            camVelocity.set(0f, 0f, 0f);
            lookVelocity.set(0f, 0f, 0f);
            initialized = true;
            return;
        }

        // map the "damping" values to smoothTime for the smooth-damp function:
        // larger damping -> snappier, so smoothTime = 1 / damping (tweakable)
        float posSmoothTime = Math.max(0.0001f, 1f / positionDamping);
        float rotSmoothTime = Math.max(0.0001f, 1f / rotationDamping);

        // Smooth the camera position using a SmoothDamp (critically-damped) algorithm
        Vector3f currentCamPos = camT.getPosition();
        Vector3f nextCamPos = smoothDamp(currentCamPos, desired, camVelocity, posSmoothTime, deltaTime);
        camT.setPosition(nextCamPos);

        // Smooth the look-at target for stable rotation
        Vector3f desiredLook = new Vector3f(playerPos.x, playerPos.y + 1.2f, playerPos.z);
        Vector3f currentLook = new Vector3f(); // build a current look target from camera's forward (or we could store it)
        // easiest: compute current look target as current cam pos + current forward direction
        // but Camera may not expose forward, so better to take desiredLook initial; we'll just smooth the raw target:
        Vector3f smoothedLook = smoothDamp(desiredLook, desiredLook, lookVelocity, rotSmoothTime, deltaTime);
        // Note: above we set target==current as placeholder to maintain lookVelocity continuity.
        // Simpler & effective: smooth the actual desired look point directly
        smoothedLook = smoothDamp(camT.getCameraFront() != null
            ? new Vector3f(camT.getPosition()).add(camT.getCameraFront())
            : desiredLook, desiredLook, lookVelocity, rotSmoothTime, deltaTime);

        // For engines without camT.getForward(), you can instead smooth desiredLook with a separate 'previousLook' stored in the system.
        camT.lookAt(smoothedLook);
    }

    /**
     * SmoothDamp for Vector3f — based on Unity's implementation.
     * currentVelocity is mutated (set to the new velocity).
     */
    private Vector3f smoothDamp(Vector3f current, Vector3f target, Vector3f currentVelocity, float smoothTime, float deltaTime) {
        // prevent divide by zero
        smoothTime = Math.max(0.0001f, smoothTime);
        float omega = 2f / smoothTime;

        float x = omega * deltaTime;
        float exp = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x);

        // change = current - target
        Vector3f change = new Vector3f(current).sub(target);
        Vector3f temp = new Vector3f(currentVelocity).add(new Vector3f(change).mul(omega)).mul(deltaTime);

        // newVelocity = (currentVelocity - omega * temp) * exp
        Vector3f newVelocity = new Vector3f(currentVelocity).sub(new Vector3f(temp).mul(omega)).mul(exp);

        // output = target + (change + temp) * exp
        Vector3f output = new Vector3f(target).add(new Vector3f(change).add(temp).mul(exp));

        // store velocity back
        currentVelocity.set(newVelocity);

        return output;
    }
}
