package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Tag;

/**
 * Marks an entity for rendering in the sprite pass (orthographic, alpha blending, no depth test).
 *
 * <p>Applied automatically by {@link org.chapzlock.core.scene.DescriptorResolver}
 * for entities with a {@link SpriteDescriptor}. Entities with this tag are collected
 * by the SpritePass and rendered as 2D screen-space quads.
 *
 * @see SpriteMaterial
 */
public class SpriteLayer implements Tag {}
