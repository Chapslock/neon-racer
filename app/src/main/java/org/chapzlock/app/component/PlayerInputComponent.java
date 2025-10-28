package org.chapzlock.app.component;

import org.chapzlock.core.application.Component;

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
