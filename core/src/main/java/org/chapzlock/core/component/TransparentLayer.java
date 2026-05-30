package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Tag;

/**
 * Marks an entity for rendering in the transparent pass (back-to-front, alpha blending).
 *
 * <p>Applied automatically by {@link org.chapzlock.core.scene.DescriptorResolver}
 * for entities with a {@link ModelDescriptor} where {@code transparent = true}.
 */
public class TransparentLayer implements Tag {}
