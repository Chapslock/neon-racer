package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.Builder;
import lombok.Getter;

/**
 * Reflection properties for specular lighting.
 *
 * <p>Controls shininess and reflectivity of a material's specular highlights.
 */
@Builder
@Getter
public class Reflection implements Component {
    private float shineDamper;
    private float reflectivity;
}
