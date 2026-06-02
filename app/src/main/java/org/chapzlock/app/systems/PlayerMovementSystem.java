package org.chapzlock.app.systems;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.util.TransformUtil;
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

    private final Vector3f tempPos = new Vector3f();
    private final Vector3f tempEuler = new Vector3f();
    private final Vector3f tempForward = new Vector3f();
    private final Vector3f tempRight = new Vector3f();

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

                TransformUtil.forward(transform, tempForward);
                Vector3f up = new Vector3f(0f, 1f, 0f);
                tempForward.cross(up, tempRight).normalize();

                Vector3f moveDir = new Vector3f();
                moveDir.add(new Vector3f(tempForward).mul(forward));
                moveDir.add(new Vector3f(tempRight).mul(right));

                if (moveDir.lengthSquared() > 0.001f) {
                    moveDir.normalize();

                    transform.getPosition(tempPos);
                    tempPos.x += moveDir.x * MOVE_SPEED * deltaTime;
                    tempPos.y += moveDir.y * MOVE_SPEED * deltaTime;
                    tempPos.z += moveDir.z * MOVE_SPEED * deltaTime;
                    transform.setPosition(tempPos);

                    if (forward != 0f) {
                        float targetYaw = (float) Math.toDegrees(Math.atan2(-moveDir.x, -moveDir.z));
                        transform.getRotationEuler(tempEuler);
                        float diff = targetYaw - tempEuler.y;

                        while (diff > 180f) { diff -= 360f; }
                        while (diff < -180f) { diff += 360f; }

                        tempEuler.y += diff * TURN_SPEED * deltaTime;
                        transform.setRotationEuler(tempEuler.x, tempEuler.y, tempEuler.z);
                    }
                }
            });
    }
}
