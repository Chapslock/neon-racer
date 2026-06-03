package org.chapzlock.app.component;

import org.chapzlock.core.ecs.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Stores the current movement input state for a humanoid player character.
 *
 * <p>Values are in the range [-1, 1] where:
 * <ul>
 *   <li>{@code moveForward}: +1 = forward (W), -1 = backward (S)</li>
 *   <li>{@code moveRight}: +1 = right (D), -1 = left (A)</li>
 * </ul>
 *
 * <p>Updated each frame by {@link org.chapzlock.app.systems.PlayerInputSystem}.
 * Consumed by {@link org.chapzlock.app.systems.CharacterMovementSystem} to move the character.
 */
@Getter
@Setter
public class PlayerInputComponent implements Component {
    float moveForward;
    float moveRight;
}
