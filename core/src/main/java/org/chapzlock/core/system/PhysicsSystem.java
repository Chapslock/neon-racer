package org.chapzlock.core.system;

import javax.vecmath.Vector3f;

import org.chapzlock.core.application.System;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.physics.EntityMotionState;
import org.chapzlock.core.physics.PhysicsCollisionUtil;
import org.chapzlock.core.physics.PhysicsDebugRenderer;
import org.chapzlock.core.physics.PhysicsDebugger;
import org.chapzlock.core.physics.PhysicsSystemSpecs;
import org.chapzlock.core.registry.ComponentRegistry;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;

/**
 * Handles the physics simulation lifecycle — initialization, updates, and optional debug rendering.
 */
public class PhysicsSystem implements System {

    private static final Vector3f DEFAULT_GRAVITY = new Vector3f(0f, -9.81f, 0f);
    private static final Vector3f WORLD_AABB_MIN = new Vector3f(-1000f, -1000f, -1000f);
    private static final Vector3f WORLD_AABB_MAX = new Vector3f(1000f, 1000f, 1000f);
    private static final float FIXED_TIME_STEP = 1f / 60f;
    private static final int MAX_SUB_STEPS = 10;

    private final ComponentRegistry registry = ComponentRegistry.instance();

    private final CollisionConfiguration collisionConfig;
    private final CollisionDispatcher dispatcher;
    private final BroadphaseInterface broadPhase;
    private final SequentialImpulseConstraintSolver solver;
    private final DiscreteDynamicsWorld dynamicsWorld;

    private final PhysicsDebugger debugger;
    private final CameraSystem cameraSystem;

    private float accumulator = 0f;
    private boolean isDebugEnabled;

    /**
     * Creates a physics system with default settings.
     */
    public PhysicsSystem() {
        this(PhysicsSystemSpecs.builder()
            .build());
    }

    /**
     * Creates a physics system based on custom specifications.
     */
    public PhysicsSystem(PhysicsSystemSpecs specs) {
        this.collisionConfig = new DefaultCollisionConfiguration();
        this.dispatcher = new CollisionDispatcher(collisionConfig);
        this.broadPhase = new AxisSweep3(WORLD_AABB_MIN, WORLD_AABB_MAX);
        this.solver = new SequentialImpulseConstraintSolver();
        this.dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadPhase, solver, collisionConfig);

        this.debugger = new PhysicsDebugger();
        this.cameraSystem = new CameraSystem();
        this.isDebugEnabled = specs.isDebugEnabled();
    }

    @Override
    public void onUpdate(float deltaTime) {
        stepSimulation(deltaTime);
        PhysicsCollisionUtil.rebuildContactCache(dynamicsWorld);
    }

    @Override
    public void onRender(float deltaTime) {
        if (isDebugEnabled) {
            renderDebugInfo();
        }
    }

    @Override
    public void onInit() {
        setupPhysicsWorld();
        registerEntities();
    }

    @Override
    public void onDestroy() {
        //TODO: Implement me!
    }

    private void setupPhysicsWorld() {
        dynamicsWorld.setGravity(DEFAULT_GRAVITY);
        dynamicsWorld.setDebugDrawer(debugger);
    }

    private void registerEntities() {
        registry.view(PhysicsBody.class, Transform.class)
            .forEach(entity -> {
                PhysicsBody body = entity.get(PhysicsBody.class);
                synchronizeEntityTransform(entity, body);
                addBodyToWorld(body);
            });
    }

    private void synchronizeEntityTransform(EntityView entity, PhysicsBody body) {
        body.getRigidBody().setMotionState(new EntityMotionState(entity.get(Transform.class)));
    }

    private void addBodyToWorld(PhysicsBody body) {
        dynamicsWorld.addRigidBody(body.getRigidBody());
    }

    private void renderDebugInfo() {
        dynamicsWorld.debugDrawWorld();
        var camera = registry.view(Camera.class).getFirst().get(Camera.class);
        var projectionMatrix = cameraSystem.calculateProjectionMatrix(camera);
        var viewMatrix = cameraSystem.calculateViewMatrix(camera);
        PhysicsDebugRenderer.drawLines(projectionMatrix, viewMatrix);
    }

    private void stepSimulation(float deltaTime) {
        accumulator += deltaTime;
        if (accumulator < FIXED_TIME_STEP) {
            return;
        }

        while (accumulator >= FIXED_TIME_STEP) {
            dynamicsWorld.stepSimulation(FIXED_TIME_STEP, MAX_SUB_STEPS);
            accumulator -= FIXED_TIME_STEP;
        }
    }
}
