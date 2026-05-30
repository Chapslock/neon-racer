package org.chapzlock.core.physics;

import javax.vecmath.Vector3f;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.event.ComponentAddedEvent;
import org.chapzlock.core.event.ComponentRemovedEvent;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.render.CameraSystem;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;

public class PhysicsSystem implements GameSystem {

    private static final Vector3f DEFAULT_GRAVITY = new Vector3f(0f, -9.81f, 0f);
    private static final Vector3f WORLD_AABB_MIN = new Vector3f(-1000f, -1000f, -1000f);
    private static final Vector3f WORLD_AABB_MAX = new Vector3f(1000f, 1000f, 1000f);
    private static final float FIXED_TIME_STEP = 1f / 60f;
    private static final int MAX_SUB_STEPS = 10;

    private ComponentRegistry registry;
    private EventBus eventBus;

    private final CollisionConfiguration collisionConfig;
    private final CollisionDispatcher dispatcher;
    private final BroadphaseInterface broadPhase;
    private final SequentialImpulseConstraintSolver solver;
    private final DiscreteDynamicsWorld dynamicsWorld;

    private final PhysicsDebugger debugger;
    private CameraSystem cameraSystem;

    private final boolean isDebugEnabled;

    private Subscription componentAddedSub;
    private Subscription componentRemovedSub;

    public PhysicsSystem() {
        this(PhysicsSystemSpecs.builder().build());
    }

    public PhysicsSystem(PhysicsSystemSpecs specs) {
        this.collisionConfig = new DefaultCollisionConfiguration();
        this.dispatcher = new CollisionDispatcher(collisionConfig);
        this.broadPhase = new AxisSweep3(WORLD_AABB_MIN, WORLD_AABB_MAX);
        this.solver = new SequentialImpulseConstraintSolver();
        this.dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadPhase, solver, collisionConfig);

        this.debugger = new PhysicsDebugger();
        this.isDebugEnabled = specs.isDebugEnabled();
    }

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.eventBus = ctx.getEventBus();
        this.cameraSystem = ctx.getCameraSystem();
        setupPhysicsWorld();
        subscribeToLifecycleEvents();
    }

    @Override
    public SystemPhase phase() {
        return SystemPhase.PHYSICS;
    }

    @Override
    public void onUpdate(float deltaTime) {
        dynamicsWorld.stepSimulation(deltaTime, MAX_SUB_STEPS, FIXED_TIME_STEP);
        PhysicsCollisionUtil.rebuildContactCache(dynamicsWorld);
    }

    @Override
    public void onDebugRender(float deltaTime) {
        if (isDebugEnabled) {
            renderDebugInfo();
        }
    }

    @Override
    public void onDestroy() {
        if (componentAddedSub != null) componentAddedSub.unsubscribe();
        if (componentRemovedSub != null) componentRemovedSub.unsubscribe();
    }

    private void setupPhysicsWorld() {
        dynamicsWorld.setGravity(DEFAULT_GRAVITY);
        dynamicsWorld.setDebugDrawer(debugger);
    }

    private void subscribeToLifecycleEvents() {
        componentAddedSub = eventBus.subscribe(ComponentAddedEvent.class, event -> {
            if (event.componentType() != PhysicsBody.class) return false;

            Transform transform = registry.getComponent(event.entityId(), Transform.class);
            if (transform == null) {
                Log.warn("PhysicsBody added to entity " + event.entityId() + " without Transform. Skipping physics registration.");
                return false;
            }

            PhysicsBody body = registry.getComponent(event.entityId(), PhysicsBody.class);
            body.getRigidBody().setMotionState(new EntityMotionState(transform));
            dynamicsWorld.addRigidBody(body.getRigidBody());
            return false;
        });

        componentRemovedSub = eventBus.subscribe(ComponentRemovedEvent.class, event -> {
            if (event.componentType() != PhysicsBody.class) return false;

            PhysicsBody body = registry.getComponent(event.entityId(), PhysicsBody.class);
            if (body != null) {
                dynamicsWorld.removeRigidBody(body.getRigidBody());
            }
            return false;
        });
    }

    private void renderDebugInfo() {
        dynamicsWorld.debugDrawWorld();
        registry.viewFirst(Camera.class).ifPresent(view -> {
            var camera = view.get(Camera.class);
            var projectionMatrix = cameraSystem.calculateProjectionMatrix(camera);
            var viewMatrix = cameraSystem.calculateViewMatrix(camera);
            PhysicsDebugRenderer.drawLines(projectionMatrix, viewMatrix);
        });
    }
}
