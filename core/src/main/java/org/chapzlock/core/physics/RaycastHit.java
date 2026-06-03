package org.chapzlock.core.physics;

import org.joml.Vector3f;

/**
 * Result of a successful raycast — contains the hit point, surface normal, distance,
 * and the entity that was struck.
 *
 * <h3>What each field means</h3>
 * <ul>
 *   <li><strong>entity</strong> — the ECS entity ID that was hit.</li>
 *   <li><strong>point</strong> — the world-space position where the ray struck the surface.</li>
 *   <li><strong>normal</strong> — the surface normal at the hit point (unit-length, points away
 *       from the surface). Useful for reflection, spawning decals at the right angle, or
 *       determining wall orientation.</li>
 *   <li><strong>distance</strong> — how far along the ray the hit occurred (from origin to point).</li>
 * </ul>
 *
 * @see Physics#raycast(Vector3f, Vector3f, float)
 */
public record RaycastHit(
    int entity,
    Vector3f point,
    Vector3f normal,
    float distance
) {}
