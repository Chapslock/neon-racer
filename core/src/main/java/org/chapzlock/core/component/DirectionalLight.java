package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector3f;

import lombok.Builder;
import lombok.Getter;

/**
 * A light source at infinite distance that illuminates all surfaces uniformly
 * from a given direction, simulating distant light sources like the sun or moon.
 *
 * <p>Unlike point lights, directional lights have no position and no attenuation —
 * every surface in the scene receives the same amount of light regardless of distance.
 * The direction vector defines where the light travels TO (pointing toward surfaces),
 * so a value of (0, -1, 0) means light shining straight down from above.</p>
 *
 * <p>One directional light per scene maximum. If multiple exist, only the first is
 * used and a warning is logged (multiple directional lights is almost always a bug).
 * Optional — scenes without one rely solely on ambient lighting and point lights.</p>
 */
@Getter
@Builder
public class DirectionalLight implements Component {

    public static final Color DEFAULT_COLOR = Color.WHITE;

    /**
     * Direction the light travels toward (not where it originates from).
     * Required — must be set explicitly via the builder.
     */
    private final Vector3f direction;

    /** Light color. Multiplied with surface color during diffuse/specular calculations. */
    @Builder.Default
    private final Color color = DEFAULT_COLOR;

    private DirectionalLight(Vector3f direction, Color color) {
        if (direction == null) {
            throw new IllegalStateException("DirectionalLight requires a direction");
        }
        this.direction = direction;
        this.color = color;
    }
}
