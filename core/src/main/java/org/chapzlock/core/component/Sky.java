package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;

import lombok.Getter;

@Getter
public class Sky implements Component {
    private Color color = new Color(153, 255, 255);
    /**
     * determines the thickness of the fog.
     * increasing this value will decrease the general visibility of the scene.
     */
    private float fogDensity = 0.007f;
    /**
     * Determines how fast the visibility decreases with distance.
     * Increasing this value will make the distance from fully visible to completely foggy smaller.
     */
    private float fogGradient = 1.5f;
}
