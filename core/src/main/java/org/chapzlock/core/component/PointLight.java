package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;
import org.joml.Vector3f;

public record PointLight(Vector3f position, Color color) implements Component {
}
