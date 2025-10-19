package org.chapzlock.core.graphics;

import org.chapzlock.core.component.Component;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Reflection implements Component {
    private float shineDamper;
    private float reflectivity;
}
