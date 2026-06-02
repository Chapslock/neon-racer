package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.Builder;
import lombok.Getter;

/**
 * A light source that radiates outward from a specific position in all directions,
 * like a light bulb, torch, or lamp post.
 *
 * <p><strong>Position:</strong> The light's world-space position comes from the entity's
 * {@link Transform} component, not from this component. This follows the ECS pattern used
 * by all major engines (Unity, Bevy, Godot, Unreal) — position is always on Transform,
 * and the light component holds only light-specific properties.
 *
 * <p><strong>Why Transform-based position?</strong>
 * <ul>
 *   <li>Movable lights just update Transform (same as any other entity)</li>
 *   <li>Lights parented to other entities (e.g., a torch on a vehicle) inherit position
 *       through the hierarchy system automatically</li>
 *   <li>Consistent with how Mesh, Camera, and PhysicsBody all use Transform for position</li>
 * </ul>
 *
 * <p><strong>Attenuation:</strong> Unlike directional lights, point light intensity decreases
 * with distance according to the {@link Attenuation} formula. Surfaces close to the light
 * appear brightly lit; surfaces far away receive little or no contribution.
 *
 * <p>The engine supports up to {@link #MAX_COUNT} simultaneous point lights per scene.
 * If more exist, only the first MAX_COUNT are sent to the GPU and a warning is logged.
 */
@Getter
@Builder
public class PointLight implements Component {

    /** Maximum number of point lights the shader supports simultaneously. */
    public static final int MAX_COUNT = 8;

    public static final Color DEFAULT_COLOR = Color.WHITE;
    public static final Attenuation DEFAULT_ATTENUATION = Attenuation.DEFAULT;

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
}
