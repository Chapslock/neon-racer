package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector3f;

/**
 * Point light source with position and color.
 *
 * @param position light position in world space
 * @param color light color
 */
public record PointLight(Vector3f position, Color color) implements Component {
}
