package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.Builder;
import lombok.Getter;

/**
 * Atmospheric fog that blends distant surfaces toward a solid color (typically the
 * sky color), simulating light scattering in the atmosphere.
 *
 * <p>Fog is calculated per-vertex based on distance from the camera using an
 * exponential formula: {@code visibility = exp(-(distance * density) ^ gradient)}.
 * At visibility=1.0 (close to camera) the surface color is fully visible; at
 * visibility=0.0 (far from camera) only the fog color (sky color) is shown.</p>
 *
 * <p>The two parameters control the fog curve:
 * <ul>
 *   <li><b>density</b> — how quickly fog accumulates with distance. Higher values
 *       make fog appear closer to the camera. A density of 0.007 starts visible
 *       fog at ~100 units from the camera.</li>
 *   <li><b>gradient</b> — how sharply the fog transitions from clear to opaque.
 *       A gradient of 1.0 produces a linear-feeling ramp; higher values (2-5) produce
 *       a sudden wall of fog at a specific distance.</li>
 * </ul>
 *
 * <p>Fog is opt-in: if no entity with a Fog component exists in the scene, fog is
 * disabled entirely (visibility = 1.0 everywhere, zero GPU cost). Add a Fog entity
 * via {@code SceneBuilder.fog()} to enable it.</p>
 */
@Getter
@Builder
public class Fog implements Component {

    public static final float DEFAULT_DENSITY = 0.007f;
    public static final float DEFAULT_GRADIENT = 1.5f;

    @Builder.Default
    private final float density = DEFAULT_DENSITY;

    @Builder.Default
    private final float gradient = DEFAULT_GRADIENT;
}
