package org.chapzlock.core.physics;

import java.util.Optional;

import javax.vecmath.Vector3f;

import org.chapzlock.core.logging.Log;
import org.joml.Quaternionf;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

/**
 * Public API for physics operations — the only class game code needs to interact with.
 *
 * <p>Obtain from {@code EngineContext.getPhysics()}. All methods accept entity IDs
 * (the same integers used throughout the ECS) and are safe to call on unregistered
 * entities (they become no-ops with a debug warning).</p>
 *
 * <h3>Forces vs Impulses (when to use which)</h3>
 * <ul>
 *   <li><strong>Force</strong> — continuous push applied every frame. Like a rocket engine
 *       that's always firing. The effect depends on how long you apply it.
 *       Use for: sustained acceleration, engines, tractor beams, wind.</li>
 *   <li><strong>Impulse</strong> — instantaneous velocity change. Like a bat hitting a ball.
 *       Applied once, has immediate effect regardless of frame rate.
 *       Use for: jumps, explosions, bullet impacts, one-shot boosts.</li>
 *   <li><strong>Torque</strong> — rotational force. Like someone spinning a top.
 *       Use for: steering, spinning, angular acceleration.</li>
 * </ul>
 *
 * <h3>Local vs World space</h3>
 * <ul>
 *   <li><strong>World space</strong> — directions relative to the scene (Y is always up).
 *       {@code applyForce(entity, new Vector3f(0, 10, 0))} always pushes up.</li>
 *   <li><strong>Local space</strong> — directions relative to the body's orientation.
 *       {@code applyLocalForce(entity, new Vector3f(0, 0, -10))} pushes "forward"
 *       relative to where the body is facing.</li>
 * </ul>
 *
 * <h3>Why a body might not respond</h3>
 * <ul>
 *   <li>Mass = 0 → static body, forces are ignored</li>
 *   <li>Body is sleeping (deactivated) → call {@link #wake(int)} first, or forces
 *       auto-wake the body</li>
 *   <li>Entity not yet registered → registration is deferred to next frame</li>
 * </ul>
 *
 * @see PhysicsBody
 * @see CollisionGeometry
 */
public class Physics {

    private final PhysicsWorld world;

    Physics(PhysicsWorld world) {
        this.world = world;
    }

    // --- Forces & Impulses ---

    public void applyForce(int entityId, org.joml.Vector3f force) {
        RigidBody body = getBodyOrWarn(entityId, "applyForce");
        if (body == null || body.isStaticObject()) return;
        body.activate(true);
        body.applyCentralForce(PhysicsUtil.toVecmath(force));
    }

    public void applyLocalForce(int entityId, org.joml.Vector3f localForce) {
        RigidBody body = getBodyOrWarn(entityId, "applyLocalForce");
        if (body == null || body.isStaticObject()) return;
        body.activate(true);
        Vector3f worldForce = localToWorldDirection(body, PhysicsUtil.toVecmath(localForce));
        body.applyCentralForce(worldForce);
    }

    public void applyForceAtPosition(int entityId, org.joml.Vector3f force, org.joml.Vector3f position) {
        RigidBody body = getBodyOrWarn(entityId, "applyForceAtPosition");
        if (body == null || body.isStaticObject()) return;
        body.activate(true);
        body.applyForce(PhysicsUtil.toVecmath(force), PhysicsUtil.toVecmath(position));
    }

    public void applyImpulse(int entityId, org.joml.Vector3f impulse) {
        RigidBody body = getBodyOrWarn(entityId, "applyImpulse");
        if (body == null || body.isStaticObject()) return;
        body.activate(true);
        body.applyCentralImpulse(PhysicsUtil.toVecmath(impulse));
    }

    public void applyLocalImpulse(int entityId, org.joml.Vector3f localImpulse) {
        RigidBody body = getBodyOrWarn(entityId, "applyLocalImpulse");
        if (body == null || body.isStaticObject()) return;
        body.activate(true);
        Vector3f worldImpulse = localToWorldDirection(body, PhysicsUtil.toVecmath(localImpulse));
        body.applyCentralImpulse(worldImpulse);
    }

    public void applyTorque(int entityId, org.joml.Vector3f torque) {
        RigidBody body = getBodyOrWarn(entityId, "applyTorque");
        if (body == null || body.isStaticObject()) return;
        body.activate(true);
        body.applyTorque(PhysicsUtil.toVecmath(torque));
    }

    public void applyLocalTorque(int entityId, org.joml.Vector3f localTorque) {
        RigidBody body = getBodyOrWarn(entityId, "applyLocalTorque");
        if (body == null || body.isStaticObject()) return;
        body.activate(true);
        Vector3f worldTorque = localToWorldDirection(body, PhysicsUtil.toVecmath(localTorque));
        body.applyTorque(worldTorque);
    }

    public void stopMotion(int entityId) {
        RigidBody body = getBodyOrWarn(entityId, "stopMotion");
        if (body == null) return;
        body.setLinearVelocity(new Vector3f(0, 0, 0));
        body.setAngularVelocity(new Vector3f(0, 0, 0));
        body.clearForces();
        body.setActivationState(CollisionObject.ISLAND_SLEEPING);
    }

    // --- Collision Queries ---

    public boolean isColliding(int entityId) {
        return world.getCollisionCache().isColliding(entityId);
    }

    public boolean areColliding(int entityA, int entityB) {
        return world.getCollisionCache().areColliding(entityA, entityB);
    }

    public boolean isCollidingWithLayer(int entityId, int layerMask) {
        return world.getCollisionCache().isCollidingWithLayer(entityId, layerMask);
    }

    public java.util.Set<Integer> getContactEntities(int entityId) {
        return world.getCollisionCache().getContactEntities(entityId);
    }

    // --- Body State ---

    public org.joml.Vector3f getLinearVelocity(int entityId) {
        RigidBody body = world.getBody(entityId);
        if (body == null) return null;
        Vector3f v = body.getLinearVelocity(new Vector3f());
        return PhysicsUtil.toJoml(v);
    }

    public org.joml.Vector3f getAngularVelocity(int entityId) {
        RigidBody body = world.getBody(entityId);
        if (body == null) return null;
        Vector3f v = body.getAngularVelocity(new Vector3f());
        return PhysicsUtil.toJoml(v);
    }

    public void setLinearVelocity(int entityId, org.joml.Vector3f velocity) {
        RigidBody body = getBodyOrWarn(entityId, "setLinearVelocity");
        if (body == null) return;
        body.activate(true);
        body.setLinearVelocity(PhysicsUtil.toVecmath(velocity));
    }

    public void setAngularVelocity(int entityId, org.joml.Vector3f velocity) {
        RigidBody body = getBodyOrWarn(entityId, "setAngularVelocity");
        if (body == null) return;
        body.activate(true);
        body.setAngularVelocity(PhysicsUtil.toVecmath(velocity));
    }

    public void teleport(int entityId, org.joml.Vector3f position) {
        RigidBody body = getBodyOrWarn(entityId, "teleport");
        if (body == null) return;
        Transform t = new Transform();
        body.getWorldTransform(t);
        t.origin.set(position.x, position.y, position.z);
        body.setWorldTransform(t);
        body.activate(true);
    }

    public void teleport(int entityId, org.joml.Vector3f position, Quaternionf rotation) {
        RigidBody body = getBodyOrWarn(entityId, "teleport");
        if (body == null) return;
        Transform t = new Transform();
        t.setIdentity();
        t.origin.set(position.x, position.y, position.z);
        t.setRotation(PhysicsUtil.toQuat4f(rotation));
        body.setWorldTransform(t);
        body.activate(true);
    }

    public boolean isRegistered(int entityId) {
        return world.isRegistered(entityId);
    }

    public boolean isSleeping(int entityId) {
        RigidBody body = world.getBody(entityId);
        return body != null && !body.isActive();
    }

    public void wake(int entityId) {
        RigidBody body = getBodyOrWarn(entityId, "wake");
        if (body != null) body.activate(true);
    }

    // --- Raycasting ---

    /**
     * Casts a ray and returns the closest hit within the given distance.
     *
     * <p>This is the simple overload — tests against all collision layers and does not
     * exclude any entity. Use the full overload for layer filtering or entity exclusion.</p>
     *
     * @param origin    ray start position in world space
     * @param direction ray direction (will be normalized internally)
     * @param maxDistance maximum distance the ray can travel
     * @return the closest hit, or empty if nothing was struck
     */
    public Optional<RaycastHit> raycast(org.joml.Vector3f origin, org.joml.Vector3f direction, float maxDistance) {
        return raycast(origin, direction, maxDistance, 0xFFFFFFFF, -1);
    }

    /**
     * Casts a ray with full control over layer filtering and entity exclusion.
     *
     * <p>The ray travels from {@code origin} in the given {@code direction} up to
     * {@code maxDistance} units. Only bodies whose layer matches the {@code layerMask}
     * bitmask are considered. If {@code excludeEntity} is >= 0, that entity is skipped
     * even if the ray hits it (useful for "don't hit yourself" scenarios).</p>
     *
     * @param origin        ray start position in world space
     * @param direction     ray direction (will be normalized internally)
     * @param maxDistance   maximum distance the ray can travel
     * @param layerMask     bitmask of collision layers to test against
     * @param excludeEntity entity ID to exclude from results, or -1 for no exclusion
     * @return the closest hit, or empty if nothing was struck
     */
    public Optional<RaycastHit> raycast(org.joml.Vector3f origin, org.joml.Vector3f direction,
                                        float maxDistance, int layerMask, int excludeEntity) {
        org.joml.Vector3f normalizedDir = new org.joml.Vector3f(direction).normalize();
        org.joml.Vector3f endpoint = new org.joml.Vector3f(origin)
            .add(normalizedDir.x * maxDistance, normalizedDir.y * maxDistance, normalizedDir.z * maxDistance);

        Vector3f from = PhysicsUtil.toVecmath(origin);
        Vector3f to = PhysicsUtil.toVecmath(endpoint);

        return world.raycast(from, to, layerMask, excludeEntity);
    }

    // --- Internal ---

    private RigidBody getBodyOrWarn(int entityId, String methodName) {
        RigidBody body = world.getBody(entityId);
        if (body == null) {
            Log.debug("Physics." + methodName + "() called on unregistered entity " + entityId + ". No-op.");
        }
        return body;
    }

    private Vector3f localToWorldDirection(RigidBody body, Vector3f localDir) {
        Transform t = new Transform();
        body.getWorldTransform(t);
        Vector3f result = new Vector3f();
        t.basis.transform(localDir, result);
        return result;
    }
}
