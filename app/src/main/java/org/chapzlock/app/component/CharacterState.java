package org.chapzlock.app.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector3f;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterState implements Component {
    private boolean grounded;
    private final Vector3f movementDirection = new Vector3f();
    private float currentSpeed;
    private float facingYaw;
}
