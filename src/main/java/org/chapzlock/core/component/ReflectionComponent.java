package org.chapzlock.core.component;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReflectionComponent implements Component {
    private float shineDamper;
    private float reflectivity;
}
