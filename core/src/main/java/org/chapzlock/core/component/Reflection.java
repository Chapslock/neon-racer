package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;

import lombok.Builder;
import lombok.Getter;

/**
 * Defines reflection properties for a material
 */
@Builder
@Getter
public class Reflection implements Component {
    private float shineDamper;
    private float reflectivity;
}
