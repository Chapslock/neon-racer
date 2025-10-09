package org.chapzlock.core.component;

import org.chapzlock.core.graphics.Color;
import org.chapzlock.core.math.Vector3f;

public record LightComponent(Vector3f position, Color color) implements Component {
}
