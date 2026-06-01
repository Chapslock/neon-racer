package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector4f;

import lombok.Builder;
import lombok.Getter;

/**
 * Describes a 2D sprite rendered in screen-space with orthographic projection.
 *
 * <p>Sprites render without depth testing and with alpha blending,
 * making them suitable for HUD elements, buttons, and overlays.
 * The descriptor is consumed during instantiation.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * SpriteDescriptor.of("textures/ui/health_bar.png")
 *
 * SpriteDescriptor.builder()
 *     .texture("textures/ui/health_bar.png")
 *     .tint(new Vector4f(1f, 0.5f, 0.5f, 0.8f))
 *     .build()
 * }</pre>
 */
@Getter
@Builder
public class SpriteDescriptor implements Component {

    /** Path to the sprite texture file. */
    private final String texture;

    /** Color multiplier applied to the texture. Default is opaque white (no tint). */
    @Builder.Default
    private final Vector4f tint = new Vector4f(1f, 1f, 1f, 1f);

    /** Convenience factory with default tint. */
    public static SpriteDescriptor of(String texture) {
        return builder().texture(texture).build();
    }
}
