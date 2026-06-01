package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Tag;

/**
 * Marks an entity for rendering in the UI pass (orthographic, no depth test).
 *
 * <p>Applied automatically by {@link org.chapzlock.core.scene.DescriptorResolver}
 * for entities with a {@link SpriteDescriptor}.
 */
public class UiLayer implements Tag {}
