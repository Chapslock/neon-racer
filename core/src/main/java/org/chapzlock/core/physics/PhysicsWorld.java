package org.chapzlock.core.physics;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import org.chapzlock.core.application.Disposable;
import org.chapzlock.core.asset.RawMeshData;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.render.MeshRegistry;
import org.joml.Quaternionf;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Owns the Bullet physics simulation and all rigid bodies within it.
 *
 * <h3>What this is (mental model)</h3>
 * <p>Think of PhysicsWorld as a "mirror universe" running alongside your visible game.
 * Each entity with a PhysicsBody gets a corresponding rigid body in this universe.
 * Every frame, the mirror universe advances time: gravity pulls things down, objects
 * collide and bounce, forces you applied take effect. Then we copy positions/rotations
 * back into the ECS so the renderer draws everything in its new spot.</p>
 *
 * <h3>Immediate registration</h3>
 * <p>Entities are registered immediately when ComponentAddedEvent fires. This is safe because
 * EntitySystem guarantees all components are present before events fire (atomic commit).
 * Removals remain deferred to avoid mid-iteration issues with the dynamics world.</p>
 *
 * <h3>Broadphase vs Narrowphase</h3>
 * <p>Collision detection is two-stage for performance:
 * <ol>
 *   <li><strong>Broadphase</strong> — uses axis-aligned bounding boxes (AABBs) to quickly
 *       find pairs of bodies that MIGHT be colliding. Very fast but imprecise.</li>
 *   <li><strong>Narrowphase</strong> — for each candidate pair from broadphase, computes
 *       exact collision geometry (contact points, penetration depth). Expensive but precise.</li>
 * </ol>
 * The {@code worldBoundsMin/Max} in config define the broadphase's spatial extent.</p>
 *
 * @see Physics
 * @see PhysicsSystem
 */
class PhysicsWorld implements Disposable {

    private final DiscreteDynamicsWorld dynamicsWorld;
    private final CollisionConfiguration collisionConfig;
    private final CollisionDispatcher dispatcher;
    private final BroadphaseInterface broadphase;
    private final SequentialImpulseConstraintSolver solver;

    private final Int2ObjectOpenHashMap<RigidBody> entityToBody = new Int2ObjectOpenHashMap<>();
    private final IdentityHashMap<RigidBody, Integer> bodyToEntity = new IdentityHashMap<>();
    private final Int2ObjectOpenHashMap<org.joml.Vector3f> entityOffsets = new Int2ObjectOpenHashMap<>();

    private final IntList pendingRemovals = new IntArrayList();

    private final CollisionCache collisionCache = new CollisionCache();
    private final CollisionShapeResolver shapeResolver = new CollisionShapeResolver();
    private final PhysicsDebugDrawer debugDrawer = new PhysicsDebugDrawer();

    private final PhysicsWorldConfig config;
    private MeshRegistry meshRegistry;

    PhysicsWorld(PhysicsWorldConfig config) {
        this.config = config;
        this.collisionConfig = new DefaultCollisionConfiguration();
        this.dispatcher = new CollisionDispatcher(collisionConfig);
        this.broadphase = new AxisSweep3(
            PhysicsUtil.toVecmath(config.worldBoundsMin()),
            PhysicsUtil.toVecmath(config.worldBoundsMax())
        );
        this.solver = new SequentialImpulseConstraintSolver();
        this.dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        this.dynamicsWorld.setGravity(PhysicsUtil.toVecmath(config.gravity()));

        if (config.debugEnabled()) {
            this.dynamicsWorld.setDebugDrawer(debugDrawer);
        }
    }

    void setMeshRegistry(MeshRegistry meshRegistry) {
        this.meshRegistry = meshRegistry;
    }

    /**
     * Registers an entity's physics body immediately into the dynamics world.
     *
     * <p>Called from the ComponentAddedEvent handler. Safe because EntitySystem guarantees
     * all components are present before events fire (atomic commit).
     *
     * @param entityId the entity to register
     * @param registry the component registry to read PhysicsBody and Transform from
     */
    void register(int entityId, ComponentRegistry registry) {
        if (entityToBody.containsKey(entityId)) return;

        PhysicsBody config = registry.getComponent(entityId, PhysicsBody.class);
        Transform transform = registry.getComponent(entityId, Transform.class);
        if (config == null || transform == null) {
            Log.warn("Physics registration skipped for entity " + entityId +
                ": missing " + (config == null ? "PhysicsBody" : "Transform"));
            return;
        }

        if (config.getGeometry() instanceof CollisionGeometry.TriangleMesh && config.getMass() != 0) {
            Log.error("Physics registration skipped for entity " + entityId +
                ": TriangleMesh geometry cannot be used with dynamic bodies (mass > 0). " +
                "TriangleMesh is a static-only shape. Use CollisionGeometry.capsule(), box(), or sphere() for dynamic bodies.");
            return;
        }

        RawMeshData meshData = resolveMeshData(entityId, registry, meshRegistry);
        CollisionShape shape = shapeResolver.resolve(config.getGeometry(), meshData);

        org.joml.Vector3f offset = extractOffset(config.getGeometry());
        if (offset != null) {
            entityOffsets.put(entityId, new org.joml.Vector3f(offset));
        }

        RigidBody body = createRigidBody(config, shape, transform, offset);
        dynamicsWorld.addRigidBody(body, (short) config.getLayer(), (short) config.getCollisionMask());

        entityToBody.put(entityId, body);
        bodyToEntity.put(body, entityId);
        collisionCache.setEntityLayer(entityId, config.getLayer());

        Log.debug("Physics body registered: entity=" + entityId +
            " mass=" + config.getMass() + " shape=" + config.getGeometry().getClass().getSimpleName());
    }

    void queueRemoval(int entityId) {
        pendingRemovals.add(entityId);
    }

    void processPendingRemovals() {
        for (int i = 0; i < pendingRemovals.size(); i++) {
            int entityId = pendingRemovals.getInt(i);
            RigidBody body = entityToBody.remove(entityId);
            if (body != null) {
                bodyToEntity.remove(body);
                dynamicsWorld.removeRigidBody(body);
                body.destroy();
                entityOffsets.remove(entityId);
                Log.debug("Physics body removed: entity=" + entityId);
            }
        }
        pendingRemovals.clear();
    }

    void step(float deltaTime) {
        dynamicsWorld.stepSimulation(deltaTime, config.maxSubSteps(), config.fixedTimeStep());
    }

    void syncTransformsToEcs(ComponentRegistry registry) {
        com.bulletphysics.linearmath.Transform bulletTransform = new com.bulletphysics.linearmath.Transform();

        entityToBody.forEach((entityId, body) -> {
            if (body.isStaticObject()) return;
            if (!body.isActive()) return;

            body.getWorldTransform(bulletTransform);
            Transform transform = registry.getComponent(entityId, Transform.class);
            if (transform == null) return;

            // Only sync rotation from physics if the body can actually rotate.
            // When angularFactor=0, game code owns rotation (e.g. character controllers).
            boolean gameOwnsRotation = Math.abs(body.getAngularFactor()) < 1e-6f;
            if (!gameOwnsRotation) {
                Quat4f q = bulletTransform.getRotation(new Quat4f());
                transform.setRotationQuaternion(PhysicsUtil.toQuaternionf(q));
            }

            org.joml.Vector3f offset = entityOffsets.get(entityId);
            if (offset != null) {
                Quaternionf rotation = new Quaternionf();
                if (gameOwnsRotation) {
                    transform.getRotationQuaternion(rotation);
                } else {
                    Quat4f q = bulletTransform.getRotation(new Quat4f());
                    rotation.set(PhysicsUtil.toQuaternionf(q));
                }
                org.joml.Vector3f rotatedOffset = new org.joml.Vector3f(offset);
                rotation.transform(rotatedOffset);
                transform.setPosition(
                    bulletTransform.origin.x - rotatedOffset.x,
                    bulletTransform.origin.y - rotatedOffset.y,
                    bulletTransform.origin.z - rotatedOffset.z);
            } else {
                transform.setPosition(
                    bulletTransform.origin.x,
                    bulletTransform.origin.y,
                    bulletTransform.origin.z);
            }
        });
    }

    void rebuildCollisionCache() {
        collisionCache.clear();

        int numManifolds = dispatcher.getNumManifolds();
        for (int i = 0; i < numManifolds; i++) {
            PersistentManifold manifold = dispatcher.getManifoldByIndexInternal(i);
            if (manifold == null || manifold.getNumContacts() == 0) continue;

            CollisionObject objA = (CollisionObject) manifold.getBody0();
            CollisionObject objB = (CollisionObject) manifold.getBody1();
            if (objA == null || objB == null) continue;

            boolean touching = false;
            for (int j = 0; j < manifold.getNumContacts(); j++) {
                ManifoldPoint mp = manifold.getContactPoint(j);
                if (mp.getDistance() <= 0f) {
                    touching = true;
                    break;
                }
            }

            if (touching) {
                Integer entityA = bodyToEntity.get(objA);
                Integer entityB = bodyToEntity.get(objB);
                if (entityA != null && entityB != null) {
                    collisionCache.recordContact(entityA, entityB);
                }
            }
        }
    }

    /**
     * Performs a raycast against the physics world and returns the closest hit.
     *
     * @param from       ray origin in world space (javax.vecmath)
     * @param to         ray endpoint in world space (javax.vecmath)
     * @param layerMask  bitmask of collision layers to test against
     * @param excludeEntity entity ID to exclude from results, or -1 for no exclusion
     * @return the closest hit, or empty if nothing was struck
     */
    Optional<RaycastHit> raycast(Vector3f from, Vector3f to, int layerMask, int excludeEntity) {
        CollisionWorld.ClosestRayResultCallback callback =
            new CollisionWorld.ClosestRayResultCallback(from, to);
        callback.collisionFilterMask = (short) layerMask;

        dynamicsWorld.rayTest(from, to, callback);

        if (!callback.hasHit()) {
            return Optional.empty();
        }

        // Resolve the hit body to an entity
        RigidBody hitBody = (RigidBody) callback.collisionObject;
        Integer hitEntity = bodyToEntity.get(hitBody);
        if (hitEntity == null) {
            return Optional.empty();
        }

        // Apply entity exclusion filter
        if (excludeEntity >= 0 && hitEntity == excludeEntity) {
            return Optional.empty();
        }

        org.joml.Vector3f hitPoint = PhysicsUtil.toJoml(callback.hitPointWorld);
        org.joml.Vector3f hitNormal = PhysicsUtil.toJoml(callback.hitNormalWorld);
        float distance = new Vector3f(
            callback.hitPointWorld.x - from.x,
            callback.hitPointWorld.y - from.y,
            callback.hitPointWorld.z - from.z
        ).length();

        return Optional.of(new RaycastHit(hitEntity, hitPoint, hitNormal, distance));
    }

    void debugDrawWorld() {
        dynamicsWorld.debugDrawWorld();
    }

    PhysicsDebugDrawer getDebugDrawer() {
        return debugDrawer;
    }

    CollisionCache getCollisionCache() {
        return collisionCache;
    }

    boolean isRegistered(int entityId) {
        return entityToBody.containsKey(entityId);
    }

    RigidBody getBody(int entityId) {
        return entityToBody.get(entityId);
    }

    Integer getEntityForBody(RigidBody body) {
        return bodyToEntity.get(body);
    }

    @Override
    public void dispose() {
        for (RigidBody body : new ArrayList<>(entityToBody.values())) {
            dynamicsWorld.removeRigidBody(body);
            body.destroy();
        }
        entityToBody.clear();
        bodyToEntity.clear();
        entityOffsets.clear();
        pendingRemovals.clear();
        debugDrawer.dispose();
    }

    private RigidBody createRigidBody(PhysicsBody config, CollisionShape shape, Transform transform,
                                      org.joml.Vector3f offset) {
        Vector3f localInertia = new Vector3f(0f, 0f, 0f);
        if (config.getMass() != 0) {
            shape.calculateLocalInertia(config.getMass(), localInertia);
        }

        com.bulletphysics.linearmath.Transform startTransform = new com.bulletphysics.linearmath.Transform();
        startTransform.setIdentity();
        org.joml.Vector3f pos = new org.joml.Vector3f();
        transform.getPosition(pos);

        if (offset != null) {
            Quaternionf rotation = new Quaternionf();
            transform.getRotationQuaternion(rotation);
            org.joml.Vector3f rotatedOffset = new org.joml.Vector3f(offset);
            rotation.transform(rotatedOffset);
            pos.add(rotatedOffset);
        }

        startTransform.origin.set(pos.x, pos.y, pos.z);

        Quaternionf engineQuat = new Quaternionf();
        transform.getRotationQuaternion(engineQuat);
        startTransform.setRotation(PhysicsUtil.toQuat4f(engineQuat));

        DefaultMotionState motionState = new DefaultMotionState(startTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(
            config.getMass(), motionState, shape, localInertia);
        RigidBody body = new RigidBody(rbInfo);
        body.setFriction(config.getFriction());
        body.setRestitution(config.getRestitution());
        body.setDamping(config.getLinearDamping(), config.getAngularDamping());
        body.setAngularFactor(config.getAngularFactor());
        return body;
    }

    /**
     * Resolves mesh data for an entity, traversing child entities if needed.
     *
     * <p>When a glTF model is loaded, the parent entity gets a PhysicsBody but no Mesh component.
     * The actual mesh data lives on descendant entities linked via the Hierarchy component. This
     * method first checks the entity's own Mesh, then recursively traverses the entire subtree
     * to collect all descendant meshes (like Unity's GetComponentsInChildren).
     *
     * @param entityId   the entity to resolve mesh data for
     * @param registry   the component registry to query
     * @param meshRegistry the mesh registry that holds raw mesh data (may be null in headless mode)
     * @return the resolved mesh data, or null if no mesh data is available
     */
    static RawMeshData resolveMeshData(int entityId, ComponentRegistry registry, MeshRegistry meshRegistry) {
        if (meshRegistry == null) return null;

        Mesh mesh = registry.getComponent(entityId, Mesh.class);
        if (mesh != null) {
            return meshRegistry.getRawMeshById(mesh.getId());
        }

        List<RawMeshData> descendantMeshes = new ArrayList<>();
        collectMeshesRecursive(entityId, registry, meshRegistry, descendantMeshes);

        if (descendantMeshes.isEmpty()) return null;
        if (descendantMeshes.size() == 1) return descendantMeshes.get(0);
        return RawMeshData.merge(descendantMeshes);
    }

    private static void collectMeshesRecursive(int entityId, ComponentRegistry registry,
                                               MeshRegistry meshRegistry, List<RawMeshData> out) {
        Hierarchy hierarchy = registry.getComponent(entityId, Hierarchy.class);
        if (hierarchy == null || hierarchy.getChildCount() == 0) return;

        int[] children = hierarchy.getChildren();
        for (int i = 0; i < children.length; i++) {
            int childId = children[i];
            Mesh childMesh = registry.getComponent(childId, Mesh.class);
            if (childMesh != null) {
                RawMeshData data = meshRegistry.getRawMeshById(childMesh.getId());
                if (data != null) {
                    out.add(data);
                }
            }
            collectMeshesRecursive(childId, registry, meshRegistry, out);
        }
    }

    private static org.joml.Vector3f extractOffset(CollisionGeometry geometry) {
        if (geometry instanceof CollisionGeometry.Offset o) {
            return o.offset();
        }
        return null;
    }
}
