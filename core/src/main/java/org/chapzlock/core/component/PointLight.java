package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector3f;

import lombok.Builder;
import lombok.Getter;

/**
 * A light source that radiates outward from a specific position in all directions,
 * like a light bulb, torch, or lamp post.
 *
 * <p>Point lights have position, color, and attenuation. Unlike directional lights,
 * their intensity decreases with distance according to the {@link Attenuation} formula.
 * Surfaces close to the light appear brightly lit; surfaces far away receive little
 * or no contribution.</p>
 *
 * <p>The engine supports up to {@link #MAX_COUNT} simultaneous point lights per scene.
 * If more exist, only the first MAX_COUNT are sent to the GPU and a warning is logged.</p>
 *
 * <p>Each point light is its own entity in the ECS. This enables runtime modification
 * (flickering effects, moving lights attached to vehicles, etc.).</p>
 */
@Getter
@Builder
public class PointLight implements Component {

    /** Maximum number of point lights the shader supports simultaneously. */
    public static final int MAX_COUNT = 8;

    public static final Color DEFAULT_COLOR = Color.WHITE;
    public static final Attenuation DEFAULT_ATTENUATION = Attenuation.DEFAULT;

    /**
     * World-space position of the light source.
     * Required — must be set explicitly via the builder.
     */
    private final Vector3f position;

    /**
     * Light color. Multiplied with surface color to produce the final lit appearance.
     */
    @Builder.Default
    private final Color color = DEFAULT_COLOR;

    /**
     * Distance falloff behavior. Controls how far this light reaches and how
     * quickly it dims. See {@link Attenuation} for preset options.
     */
    @Builder.Default
    private final Attenuation attenuation = DEFAULT_ATTENUATION;

    private PointLight(Vector3f position, Color color, Attenuation attenuation) {
        if (position == null) {
            throw new IllegalStateException("PointLight requires a position");
        }
        this.position = position;
        this.color = color;
        this.attenuation = attenuation;
    }
}
