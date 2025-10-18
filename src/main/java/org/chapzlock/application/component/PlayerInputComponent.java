package org.chapzlock.application.component;

import org.chapzlock.core.component.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class PlayerInputComponent implements Component {
    boolean isMovingLeft;
    boolean isMovingRight;
    boolean isMovingForward;
    boolean isMovingBackwards;
}
