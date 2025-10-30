package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;

import lombok.Getter;

@Getter
public class Sky implements Component {
    private Color color = new Color(153, 255, 255);
}
