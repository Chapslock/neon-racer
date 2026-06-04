package org.chapzlock.core.physics;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.event.ComponentAddedEvent;
import org.chapzlock.core.event.ComponentRemovedEvent;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.render.CameraProjection;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Lifecycle system that connects the physics simulation to the ECS game loop.
 *
 * <h3>What this system does each frame</h3>
 * <ol>
 *   <li><strong>Process pending registrations</strong> — drains the queue of entities that
 *       received a PhysicsBody since last frame, registering them into the simulation. This
 *       deferred approach guarantees glTF child meshes are loaded before mesh-dependent shapes
 *       (TriangleMesh, ConvexHull) attempt to resolve geometry.</li>
 *   <li><strong>Process removals</strong> — any entities that lost their PhysicsBody get
 *       removed from the simulation.</li>
 *   <li><strong>Step simulation</strong> — advances the physics world by deltaTime. Internally
 *       this runs at a fixed 60Hz with sub-stepping to stay frame-rate-independent.</li>
 *   <li><strong>Sync transforms</strong> — copies positions/rotations from the physics world
 *       back into ECS Transform components so the renderer draws things in the right place.</li>
 *   <li><strong>Rebuild collision cache</strong> — scans all contact manifolds and builds the
 *       fast entity-ID-based lookup used by {@code Physics.isColliding()}.</li>
 * </ol>
 *
 * <h3>Registration strategy (deferred queue)</h3>
 * <p>When ComponentAddedEvent fires for a PhysicsBody, the entity ID is added to a pending
 * queue rather than registered immediately. The queue is drained at the start of
 * {@code onUpdate()}, at which point all scene loading is complete and mesh data is available.
 * This matches the standard approach used by Unity (FixedUpdate), Godot (PhysicsServer step),
 * and Bevy (schedule ordering).</p>
 *
 * <p>Entities that are destroyed or have their PhysicsBody removed before the queue drains
 * are silently skipped.</p>
 *
 * @see Physics
 * @see PhysicsWorld
 * @see PhysicsWorldConfig
 */
public class PhysicsSystem implements GameSystem {

    private final PhysicsWorldConfig config;

    private PhysicsWorld world;
    private ComponentRegistry registry;
    private CameraProjection cameraProjection;

    private Subscription componentAddedSub;
    private Subscription componentRemovedSub;

    private final IntList pendingRegistrations = new IntArrayList();

    public PhysicsSystem() {
        this(PhysicsWorldConfig.defaults());
    }

    public PhysicsSystem(PhysicsWorldConfig config) {
        this.config = config;
    }

    public PhysicsSystem(PhysicsWorldConfig config, CameraProjection cameraProjection) {
        this.config = config;
        this.cameraProjection = cameraProjection;
    }

    public PhysicsSystem(PhysicsWorldConfig config, CameraProjection cameraProjection, PhysicsWorld world) {
        this.config = config;
        this.cameraProjection = cameraProjection;
        this.world = world;
    }

    @Override
    public void onInit(Engine engine) {
        this.registry = engine.getRegistry();
        if (this.cameraProjection == null) {
            this.cameraProjection = engine.getCameraProjection();
        }
        if (this.world == null) {
            // Fallback for cases where PhysicsSystem is used without Engine.create()
            this.world = new PhysicsWorld(config);
            this.world.setMeshRegistry(engine.getMeshRegistry());
            Physics physics = new Physics(world);
            engine.setPhysics(physics);
        }
        subscribeToLifecycleEvents(engine.getEventBus());
    }

    @Override
    public SystemPhase phase() {
        return SystemPhase.PHYSICS;
    }

    @Override
    public void onUpdate(float deltaTime) {
        processPendingRegistrations();
        world.processPendingRemovals();
        world.step(deltaTime);
        world.syncTransformsToEcs(registry);
        world.rebuildCollisionCache();
    }

    @Override
    public void onDebugRender(float deltaTime) {
        if (!config.debugEnabled()) return;

        world.debugDrawWorld();
        final Camera[] cam = {null};
        final Transform[] camTransform = {null};
        registry.forEach(Camera.class, Transform.class, (entityId, camera, transform) -> {
            if (cam[0] == null) {
                cam[0] = camera;
                camTransform[0] = transform;
            }
        });
        if (cam[0] != null) {
            var projectionMatrix = cameraProjection.calculateProjectionMatrix(cam[0]);
            var viewMatrix = cameraProjection.calculateViewMatrix(camTransform[0]);
            world.getDebugDrawer().render(projectionMatrix, viewMatrix);
        }
    }

    @Override
    public void onDestroy() {
        if (componentAddedSub != null) componentAddedSub.unsubscribe();
        if (componentRemovedSub != null) componentRemovedSub.unsubscribe();
        if (world != null) world.dispose();
    }

    private void processPendingRegistrations() {
        for (int i = 0; i < pendingRegistrations.size(); i++) {
            int entityId = pendingRegistrations.getInt(i);
            if (!registry.entityExists(entityId)) continue;
            if (registry.getComponent(entityId, PhysicsBody.class) == null) continue;
            world.register(entityId, registry);
        }
        pendingRegistrations.clear();
    }

    private void subscribeToLifecycleEvents(EventBus eventBus) {
        componentAddedSub = eventBus.subscribe(ComponentAddedEvent.class, event -> {
            if (event.componentType() != PhysicsBody.class) return false;
            pendingRegistrations.add(event.entityId());
            return false;
        });

        componentRemovedSub = eventBus.subscribe(ComponentRemovedEvent.class, event -> {
            if (event.componentType() != PhysicsBody.class) return false;
            world.queueRemoval(event.entityId());
            return false;
        });
    }
}
