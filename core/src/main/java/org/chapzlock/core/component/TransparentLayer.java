package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Tag;

/**
 * Marks an entity for rendering in the transparent pass (back-to-front, alpha blending).
 *
 * <p>Applied automatically during glTF scene instantiation for transparent meshes.
 */
public class TransparentLayer implements Tag {}
