package org.chapzlock.app.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector3f;

import lombok.Getter;
import lombok.Setter;

/**
 * Runtime state shared between character movement systems.
 *
 * <p>Written by:
 * <ul>
 *   <li>{@link org.chapzlock.app.systems.CharacterGroundCheckSystem} — sets {@code grounded}</li>
 *   <li>{@link org.chapzlock.app.systems.CharacterMovementSystem} — sets {@code movementDirection}, {@code currentSpeed}</li>
 *   <li>{@link org.chapzlock.app.systems.CharacterRotationSystem} — sets {@code facingYaw}</li>
 * </ul>
 *
 * <p>Read by:
 * <ul>
 *   <li>{@link org.chapzlock.app.systems.CharacterJumpSystem} — reads {@code grounded}</li>
 *   <li>{@link org.chapzlock.app.systems.CharacterAnimationSystem} — reads {@code currentSpeed}</li>
 *   <li>{@link org.chapzlock.app.systems.ThirdPersonCameraSystem} — reads {@code facingYaw}</li>
 * </ul>
 */
@Getter
@Setter
public class CharacterState implements Component {
    private boolean grounded;
    private final Vector3f movementDirection = new Vector3f();
    private float currentSpeed;
    private float facingYaw;
}
