package org.chapzlock.app.systems;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.joml.Vector3f;

/**
 * Moves the humanoid player character based on input from {@link PlayerInputComponent}.
 *
 * <p>Movement is transform-based (no physics forces): the player moves at a constant
 * speed in the direction they are facing, and strafes perpendicular to that direction.
 * The character rotates to face the movement direction when moving forward/backward.
 */
public class PlayerMovementSystem implements GameSystem {

    private ComponentRegistry registry;

    private static final float MOVE_SPEED = 8.0f;
    private static final float TURN_SPEED = 10.0f;

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        registry.forEach(PlayerTag.class, PlayerInputComponent.class, Transform.class,
            (entityId, tag, input, transform) -> {
                float forward = input.getMoveForward();
                float right = input.getMoveRight();

                if (forward == 0f && right == 0f) {
                    return;
                }

                Vector3f playerForward = transform.getForwardVector(new Vector3f());
                Vector3f up = new Vector3f(0f, 1f, 0f);
                Vector3f playerRight = new Vector3f();
                playerForward.cross(up, playerRight).normalize();

                Vector3f moveDir = new Vector3f();
                moveDir.add(new Vector3f(playerForward).mul(forward));
                moveDir.add(new Vector3f(playerRight).mul(right));

                if (moveDir.lengthSquared() > 0.001f) {
                    moveDir.normalize();

                    Vector3f pos = transform.getPosition();
                    pos.x += moveDir.x * MOVE_SPEED * deltaTime;
                    pos.y += moveDir.y * MOVE_SPEED * deltaTime;
                    pos.z += moveDir.z * MOVE_SPEED * deltaTime;

                    if (forward != 0f) {
                        float targetYaw = (float) Math.toDegrees(Math.atan2(-moveDir.x, -moveDir.z));
                        float currentYaw = transform.getRotationEuler().y;
                        float diff = targetYaw - currentYaw;

                        while (diff > 180f) { diff -= 360f; }
                        while (diff < -180f) { diff += 360f; }

                        transform.getRotationEuler().y += diff * TURN_SPEED * deltaTime;
                    }
                }
            });
    }
}
