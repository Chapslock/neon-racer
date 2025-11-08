package org.chapzlock.core.physics;


import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import lombok.experimental.UtilityClass;

/**
 * Utility class for applying forces and impulses to physics bodies.
 * <p>
 * Designed to be clean, performant, and safe to use in gameplay systems.
 */
@UtilityClass
public final class PhysicsForceUtil {

    // Temp reusable vectors for minor optimization (avoid new allocations).
    // Not thread-safe — call only from the physics thread.
    private static final Vector3f tempWorldDir = new Vector3f();
    private static final Transform tempLocalDir = new Transform();
    private static final Vector3f tempWorldPos = new Vector3f();

    /**
     * Applies a continuous world-space force.
     */
    public static void applyForce(RigidBody body, org.joml.Vector3f force) {
        Vector3f vec = PhysicsMapper.vector3f(force);
        if (!isValidDynamic(body, vec)) {
            return;
        }
        body.activate(true);
        body.applyCentralForce(vec);
    }

    private static boolean isValidDynamic(RigidBody body, Vector3f vec) {
        if (body == null || body.isStaticObject()) {
            return false;
        }
        return vec != null && (vec.x != 0f || vec.y != 0f || vec.z != 0f);
    }

    /**
     * Applies an instantaneous world-space impulse (velocity change).
     */
    public static void applyImpulse(RigidBody body, Vector3f impulse) {
        if (!isValidDynamic(body, impulse)) {
            return;
        }
        body.activate(true);
        body.applyCentralImpulse(impulse);
    }

    /**
     * Applies a continuous force in the body's local space (e.g., forward).
     */
    public static void applyLocalForce(RigidBody body, org.joml.Vector3f localForce) {
        Vector3f force = PhysicsMapper.vector3f(localForce);
        if (!isValidDynamic(body, force)) {
            return;
        }
        body.activate(true);
        transformLocalToWorldDir(body, force, tempWorldDir);
        body.applyCentralForce(tempWorldDir);
    }

    /**
     * Applies a force given in the body's local space at a local-space position (convenience).
     * Useful to push at wheel positions: will produce torque (steering roll/yaw).
     */
    public static void applyLocalForceAtPosition(RigidBody body, org.joml.Vector3f localForce, org.joml.Vector3f localPosition) {
        Vector3f force = PhysicsMapper.vector3f(localForce);
        Vector3f posLocal = PhysicsMapper.vector3f(localPosition);
        if (!isValidDynamic(body, force) || posLocal == null) {
            return;
        }
        body.activate(true);
        transformLocalToWorldDir(body, force, tempWorldDir);
        transformLocalToWorldPoint(body, posLocal);
        body.applyForce(tempWorldDir, tempWorldPos);
    }

    /**
     * Converts a local-space direction vector into world-space direction.
     */
    private static void transformLocalToWorldDir(RigidBody body, Vector3f local, Vector3f out) {
        // fill tempLocalDir with world transform
        body.getWorldTransform(tempLocalDir);
        // use the basis (rotation) to transform direction (no translation)
        tempLocalDir.basis.transform(local, out);
    }

    /**
     * Applies an instantaneous impulse in the body's local space.
     */
    public static void applyLocalImpulse(RigidBody body, Vector3f localImpulse) {
        if (!isValidDynamic(body, localImpulse)) {
            return;
        }
        body.activate(true);
        transformLocalToWorldDir(body, localImpulse, tempWorldDir);
        body.applyCentralImpulse(tempWorldDir);
    }

    /**
     * Applies a force at a specific world position (produces torque).
     */
    public static void applyForceAtPosition(RigidBody body, Vector3f force, Vector3f position) {
        if (!isValidDynamic(body, force) || position == null) {
            return;
        }
        body.activate(true);
        body.applyForce(force, position);
    }

    /**
     * Converts a local-space point into a world-space point (includes translation).
     */
    private static void transformLocalToWorldPoint(RigidBody body, Vector3f localPoint) {
        body.getWorldTransform(tempLocalDir);
        // Transform point (includes translation)
        tempLocalDir.transform(localPoint);
    }

    /**
     * Applies torque (rotational force) in world space.
     */
    public static void applyTorque(RigidBody body, Vector3f torque) {
        if (!isValidDynamic(body, torque)) {
            return;
        }
        body.activate(true);
        body.applyTorque(torque);
    }

    /**
     * Applies a torque specified in local space (converted to world).
     * e.g., rotate around the local up axis.
     */
    public static void applyLocalTorque(RigidBody body, org.joml.Vector3f localTorque) {
        Vector3f t = PhysicsMapper.vector3f(localTorque);
        if (!isValidDynamic(body, t)) {
            return;
        }
        body.activate(true);
        transformLocalToWorldDir(body, t, tempWorldDir);
        body.applyTorque(tempWorldDir);
    }

    /**
     * Stops all movement (useful for resetting or freezing entities).
     */
    public static void stopMotion(RigidBody body) {
        if (body == null) {
            return;
        }
        body.activate(true);
        body.setLinearVelocity(new Vector3f(0, 0, 0));
        body.setAngularVelocity(new Vector3f(0, 0, 0));
        body.clearForces();
    }
}
