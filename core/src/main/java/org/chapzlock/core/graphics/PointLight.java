package org.chapzlock.core.graphics;

import org.chapzlock.core.component.Component;
import org.joml.Vector3f;

public record PointLight(Vector3f position, Color color) implements Component {
}
