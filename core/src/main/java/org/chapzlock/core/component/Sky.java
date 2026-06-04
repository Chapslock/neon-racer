package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.Getter;

/**
 * Sky/background component defining the scene's clear color.
 *
 * <p>Only one Sky component should exist per scene. Controls what color
 * fills the background behind all rendered geometry.
 *
 * <p>For distance fog, use the separate {@link Fog} component.
 */
@Getter
public class Sky implements Component {
    private Color color = new Color(153, 255, 255);

    public Sky() {}

    public Sky(Color color) {
        this.color = color;
    }
}
