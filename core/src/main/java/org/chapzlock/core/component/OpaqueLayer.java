package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Tag;

/**
 * Marks an entity for rendering in the opaque pass (front-to-back, no blending).
 *
 * <p>Applied automatically during glTF scene instantiation for opaque meshes.
 */
public class OpaqueLayer implements Tag {}
