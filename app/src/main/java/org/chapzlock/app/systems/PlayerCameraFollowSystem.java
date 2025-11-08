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
        Vector3f playerForward = playerT.getForwardVector();

        Vector3f desired = new Vector3f(
            playerPos.x - playerForward.x * followDistance + lateralOffset,
            playerPos.y + height,
            playerPos.z - playerForward.z * followDistance
        );

        // linear interpolation
        float t = 1f - (float) Math.exp(-positionDamping * deltaTime);
        Vector3f current = camT.getPosition();
        Vector3f next = new Vector3f(
            current.x + (desired.x - current.x) * t,
            current.y + (desired.y - current.y) * t,
            current.z + (desired.z - current.z) * t
        );
        camT.setPosition(next);

        Vector3f lookTarget = new Vector3f(playerPos.x, playerPos.y + 1.2f, playerPos.z);
        camT.lookAt(lookTarget);
    }
}
