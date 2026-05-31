package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Tag;

/**
 * Marks an entity for rendering in the opaque pass (front-to-back, no blending).
 *
 * <p>Applied automatically by {@link org.chapzlock.core.scene.DescriptorResolver}
 * for entities with a {@link ModelDescriptor} (non-transparent).
 */
public class OpaqueLayer implements Tag {}
