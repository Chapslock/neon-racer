package org.chapzlock.app.component;

import org.chapzlock.core.ecs.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerInputComponent implements Component {
    float throttle;
    float steer;
}
