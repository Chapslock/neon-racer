package org.chapzlock.core.physics;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.event.ComponentAddedEvent;
import org.chapzlock.core.event.ComponentRemovedEvent;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.render.CameraSystem;

/**
 * Lifecycle system that connects the physics simulation to the ECS game loop.
 *
 * <h3>What this system does each frame</h3>
 * <ol>
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
 * <h3>Registration strategy</h3>
 * <p>Entities are registered immediately when ComponentAddedEvent fires. This is safe because
 * EntitySystem guarantees all components are present before events fire (atomic commit).
 * Removals remain deferred to avoid mid-iteration issues with the dynamics world.</p>
 *
 * @see Physics
 * @see PhysicsWorld
 * @see PhysicsWorldConfig
 */
public class PhysicsSystem implements GameSystem {

    private final PhysicsWorldConfig config;

    private PhysicsWorld world;
    private ComponentRegistry registry;
    private CameraSystem cameraSystem;

    private Subscription componentAddedSub;
    private Subscription componentRemovedSub;

    public PhysicsSystem() {
        this(PhysicsWorldConfig.defaults());
    }

    public PhysicsSystem(PhysicsWorldConfig config) {
        this.config = config;
    }

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.cameraSystem = ctx.getCameraSystem();

        this.world = new PhysicsWorld(config);
        this.world.setMeshSystem(ctx.getMeshSystem());

        Physics physics = new Physics(world);
        ctx.setPhysics(physics);

        subscribeToLifecycleEvents(ctx.getEventBus());
    }

    @Override
    public SystemPhase phase() {
        return SystemPhase.PHYSICS;
    }

    @Override
    public void onUpdate(float deltaTime) {
        world.processPendingRemovals();
        world.step(deltaTime);
        world.syncTransformsToEcs(registry);
        world.rebuildCollisionCache();
    }

    @Override
    public void onDebugRender(float deltaTime) {
        if (!config.debugEnabled()) return;

        world.debugDrawWorld();
        registry.getSingleton(Camera.class).ifPresent(camera -> {
            var projectionMatrix = cameraSystem.calculateProjectionMatrix(camera);
            var viewMatrix = cameraSystem.calculateViewMatrix(camera);
            world.getDebugDrawer().render(projectionMatrix, viewMatrix);
        });
    }

    @Override
    public void onDestroy() {
        if (componentAddedSub != null) componentAddedSub.unsubscribe();
        if (componentRemovedSub != null) componentRemovedSub.unsubscribe();
        if (world != null) world.dispose();
    }

    private void subscribeToLifecycleEvents(EventBus eventBus) {
        componentAddedSub = eventBus.subscribe(ComponentAddedEvent.class, event -> {
            if (event.componentType() != PhysicsBody.class) return false;
            world.register(event.entityId(), registry);
            return false;
        });

        componentRemovedSub = eventBus.subscribe(ComponentRemovedEvent.class, event -> {
            if (event.componentType() != PhysicsBody.class) return false;
            world.queueRemoval(event.entityId());
            return false;
        });
    }
}
