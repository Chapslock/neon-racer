package org.chapzlock.core.physics;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;

import lombok.experimental.UtilityClass;

/**
 * High-performance collision utilities.
 * <p>
 * Features:
 * - O(1) cached lookups for "is colliding?" queries after each physics step
 * - Zero allocations during queries
 * - NB! Make sure you are running s physics simulation in the background.
 */
@UtilityClass
public class PhysicsCollisionUtil {

    /**
     * Cached contact map: each object → set of others it’s touching
     */
    private static final Map<CollisionObject, Set<CollisionObject>> contactMap = new IdentityHashMap<>();

    /**
     * Should be called once per physics step (after world.stepSimulation)
     * to rebuild the contact cache from all manifolds.
     */
    public static void rebuildContactCache(DiscreteDynamicsWorld world) {
        contactMap.clear();

        if (!(world.getDispatcher() instanceof CollisionDispatcher)) {
            return;
        }
        CollisionDispatcher dispatcher = (CollisionDispatcher) world.getDispatcher();
        int numManifolds = dispatcher.getNumManifolds();

        for (int i = 0; i < numManifolds; i++) {
            PersistentManifold manifold = dispatcher.getManifoldByIndexInternal(i);
            if (manifold == null || manifold.getNumContacts() == 0) {
                continue;
            }

            CollisionObject a = (CollisionObject) manifold.getBody0();
            CollisionObject b = (CollisionObject) manifold.getBody1();
            if (a == null || b == null) {
                continue;
            }

            boolean isTouching = false;
            int contacts = manifold.getNumContacts();
            for (int j = 0; j < contacts; j++) {
                ManifoldPoint mp = manifold.getContactPoint(j);
                if (mp.getDistance() <= 0f) {
                    isTouching = true;
                    break;
                }
            }

            if (isTouching) {
                contactMap.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                contactMap.computeIfAbsent(b, k -> new HashSet<>()).add(a);
            }
        }
    }

    /**
     * Cached check: O(1) average time after calling rebuildContactCache().
     */
    public static boolean areColliding(CollisionObject a, CollisionObject b) {
        Set<CollisionObject> others = contactMap.get(a);
        return others != null && others.contains(b);
    }

    /**
     * Cached check: is subject colliding with *any* object satisfying filter?
     */
    public static boolean isCollidingWithAny(CollisionObject subject, Predicate<CollisionObject> filter) {
        Set<CollisionObject> others = contactMap.get(subject);
        if (others == null) {
            return false;
        }
        if (filter == null) {
            return !others.isEmpty();
        }
        for (CollisionObject o : others) {
            if (filter.test(o)) {
                return true;
            }
        }
        return false;
    }
}

