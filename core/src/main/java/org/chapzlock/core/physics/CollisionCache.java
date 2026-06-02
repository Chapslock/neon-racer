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

    void recordContact(int entityA, int entityB) {
        contacts.computeIfAbsent(entityA, k -> new HashSet<>()).add(entityB);
        contacts.computeIfAbsent(entityB, k -> new HashSet<>()).add(entityA);
    }

    void setEntityLayer(int entityId, int layer) {
        entityLayers.put(entityId, layer);
    }

    void clear() {
        contacts.clear();
    }

    public boolean areColliding(int entityA, int entityB) {
        Set<Integer> set = contacts.get(entityA);
        return set != null && set.contains(entityB);
    }

    public boolean isColliding(int entityId) {
        Set<Integer> set = contacts.get(entityId);
        return set != null && !set.isEmpty();
    }

    public boolean isCollidingWithLayer(int entityId, int layerMask) {
        Set<Integer> set = contacts.get(entityId);
        if (set == null) return false;
        for (int other : set) {
            int otherLayer = entityLayers.getOrDefault(other, 1);
            if ((otherLayer & layerMask) != 0) return true;
        }
        return false;
    }

    public Set<Integer> getContactEntities(int entityId) {
        Set<Integer> set = contacts.get(entityId);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }
}
