package org.chapzlock.application.component;

import org.chapzlock.core.component.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class InputComponent implements Component {
    boolean isMovingLeft, isMovingRight, isMovingForward, isMovingBackwards;
}
