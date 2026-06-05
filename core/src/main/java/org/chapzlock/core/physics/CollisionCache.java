package org.chapzlock.core.physics;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Caches which entities are in physical contact each frame for fast collision queries.
 *
 * <h3>How collision detection works (simplified)</h3>
 * <p>Each physics step, the engine detects all pairs of bodies that are touching.
 * These pairs are stored in "manifolds" (contact manifold = the surface patch where
 * two shapes overlap). After stepping, this cache reads all manifolds and builds a
 * fast lookup map: entity → set of entities it's touching.</p>
 *
 * <h3>Why a cache instead of querying Bullet directly?</h3>
 * <p>Bullet stores contacts in a format optimized for the solver, not for game logic queries.
 * Rebuilding this cache once per frame (O(manifolds)) gives O(1) lookups for game systems
 * that check collisions many times per frame (e.g., "is the player touching the ground?").</p>
 *
 * <h3>Staleness</h3>
 * <p>The cache represents contacts from the LAST physics step. If you check collisions
 * during the same frame but before the physics step runs, you're reading last frame's data.
 * This is fine for gameplay — one frame of latency is imperceptible.</p>
 *
 * @see Physics#isColliding(int)
 * @see Physics#areColliding(int, int)
 */
public class CollisionCache {

    private final Map<Integer, Set<Integer>> contacts = new HashMap<>();
    private final Map<Integer, Integer> entityLayers = new HashMap<>();

    /**
     * Records a contact between two entities. Called by {@link PhysicsSystem} after stepping
     * the physics world, once for each active contact manifold.
     *
     * <p>Contacts are stored bidirectionally: if A touches B, both A's and B's contact sets
     * are updated.</p>
     *
     * @param entityA the first entity in the contact pair
     * @param entityB the second entity in the contact pair
     */
    void recordContact(int entityA, int entityB) {
        contacts.computeIfAbsent(entityA, k -> new HashSet<>()).add(entityB);
        contacts.computeIfAbsent(entityB, k -> new HashSet<>()).add(entityA);
    }

    /**
     * Associates an entity with a collision layer bitmask. Layers allow game systems to query
     * contacts filtered by category (e.g., "is the player touching anything on the ground layer?").
     *
     * @param entityId the entity whose layer is being set
     * @param layer    the collision layer bitmask for this entity
     */
    void setEntityLayer(int entityId, int layer) {
        entityLayers.put(entityId, layer);
    }

    /**
     * Clears all recorded contacts. Called at the start of each physics step before rebuilding
     * the contact set from the current frame's manifolds.
     */
    void clear() {
        contacts.clear();
    }

    /**
     * Checks whether two specific entities are currently in contact.
     *
     * @param entityA the first entity to check
     * @param entityB the second entity to check
     * @return {@code true} if the two entities are touching according to the last physics step
     */
    public boolean areColliding(int entityA, int entityB) {
        Set<Integer> set = contacts.get(entityA);
        return set != null && set.contains(entityB);
    }

    /**
     * Checks whether an entity is in contact with any other entity.
     *
     * @param entityId the entity to check
     * @return {@code true} if the entity is touching at least one other entity
     */
    public boolean isColliding(int entityId) {
        Set<Integer> set = contacts.get(entityId);
        return set != null && !set.isEmpty();
    }

    /**
     * Checks whether an entity is in contact with any entity belonging to a specific layer.
     *
     * <p>The check uses bitwise AND between the other entity's layer and the provided mask.
     * For example, if ground entities have layer {@code 0x02}, pass {@code 0x02} to check
     * whether the player is touching the ground.</p>
     *
     * @param entityId  the entity to check contacts for
     * @param layerMask a bitmask representing the target collision layers to test against
     * @return {@code true} if at least one contacting entity matches the layer mask
     */
    public boolean isCollidingWithLayer(int entityId, int layerMask) {
        Set<Integer> set = contacts.get(entityId);
        if (set == null) return false;
        for (int other : set) {
            int otherLayer = entityLayers.getOrDefault(other, 1);
            if ((otherLayer & layerMask) != 0) return true;
        }
        return false;
    }

    /**
     * Returns the set of all entity IDs currently in contact with the given entity.
     *
     * @param entityId the entity whose contacts are being queried
     * @return an unmodifiable set of entity IDs in contact, or an empty set if none
     */
    public Set<Integer> getContactEntities(int entityId) {
        Set<Integer> set = contacts.get(entityId);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }
}
