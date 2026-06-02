package org.chapzlock.app.systems;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.util.TransformUtil;
import org.joml.Vector3f;


public class PlayerCameraFollowSystem implements GameSystem {

    private ComponentRegistry registry;

    private final float followDistance = 6f;
    private final float height = 2.5f;
    private final float lateralOffset = 0.0f;
    private final float positionDamping = 6f;
    private final float rotationDamping = 8f;

    private final Vector3f camVelocity = new Vector3f();
    private final Vector3f lookVelocity = new Vector3f();
    private boolean initialized = false;

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        var cameraOpt = registry.getSingleton(Camera.class);
        int playerId = registry.getEntityByName("player");

        if (cameraOpt.isEmpty() || playerId == -1) {
            return;
        }

        Camera camT = cameraOpt.get();
        Transform playerT = registry.getComponent(playerId, Transform.class);
        if (playerT == null) {
            return;
        }

        Vector3f playerPos = new Vector3f();
        playerT.getPosition(playerPos);
        Vector3f playerForward = TransformUtil.forward(playerT, new Vector3f());

        Vector3f up = new Vector3f(0f, 1f, 0f);
        Vector3f right = new Vector3f();
        playerForward.cross(up, right);
        if (right.lengthSquared() < 1e-6f) {
            right.set(1f, 0f, 0f);
        } else {
            right.normalize();
        }
        Vector3f lateral = new Vector3f(right).mul(lateralOffset);

        Vector3f desired = new Vector3f(
            playerPos.x - playerForward.x * followDistance,
            playerPos.y + height,
            playerPos.z - playerForward.z * followDistance
        );
        desired.add(lateral);

        if (!initialized) {
            camT.setPosition(desired);
            Vector3f initialLook = new Vector3f(playerPos.x, playerPos.y + 1.2f, playerPos.z);
            camT.lookAt(initialLook);
            camVelocity.set(0f, 0f, 0f);
            lookVelocity.set(0f, 0f, 0f);
            initialized = true;
            return;
        }

        float posSmoothTime = Math.max(0.0001f, 1f / positionDamping);
        float rotSmoothTime = Math.max(0.0001f, 1f / rotationDamping);

        Vector3f currentCamPos = camT.getPosition();
        Vector3f nextCamPos = smoothDamp(currentCamPos, desired, camVelocity, posSmoothTime, deltaTime);
        camT.setPosition(nextCamPos);

        Vector3f desiredLook = new Vector3f(playerPos.x, playerPos.y + 1.2f, playerPos.z);
        Vector3f smoothedLook = smoothDamp(camT.getCameraFront() != null
            ? new Vector3f(camT.getPosition()).add(camT.getCameraFront())
            : desiredLook, desiredLook, lookVelocity, rotSmoothTime, deltaTime);

        camT.lookAt(smoothedLook);
    }

    private Vector3f smoothDamp(Vector3f current, Vector3f target, Vector3f currentVelocity, float smoothTime, float deltaTime) {
        smoothTime = Math.max(0.0001f, smoothTime);
        float omega = 2f / smoothTime;

        float x = omega * deltaTime;
        float exp = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x);

        Vector3f change = new Vector3f(current).sub(target);
        Vector3f temp = new Vector3f(currentVelocity).add(new Vector3f(change).mul(omega)).mul(deltaTime);

        Vector3f newVelocity = new Vector3f(currentVelocity).sub(new Vector3f(temp).mul(omega)).mul(exp);

        Vector3f output = new Vector3f(target).add(new Vector3f(change).add(temp).mul(exp));

        currentVelocity.set(newVelocity);

        return output;
    }
}
