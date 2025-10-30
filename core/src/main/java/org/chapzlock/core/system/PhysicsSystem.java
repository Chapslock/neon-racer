package org.chapzlock.core.system;

import javax.vecmath.Vector3f;

import org.chapzlock.core.application.System;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.physics.EntityMotionState;
import org.chapzlock.core.registry.ComponentRegistry;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;

public class PhysicsSystem implements System {

    private static final Vector3f DEFAULT_GRAVITY = new Vector3f(0f, -9.81f, 0f);
    private final ComponentRegistry registry = ComponentRegistry.instance();

    private final CollisionConfiguration collisionConfig = new DefaultCollisionConfiguration();
    private final CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfig);
    private final DiscreteDynamicsWorld dynamicsWorld = new DiscreteDynamicsWorld(
        dispatcher,
        broadPhase,
        solver,
        collisionConfig
    );
    // world AABB for AxisSweep3 - keep large enough for your game world
    private final BroadphaseInterface broadPhase = new AxisSweep3(
        new Vector3f(-1000f, -1000f, -1000f),
        new Vector3f(1000f, 1000f, 1000f)
    );
    private final SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();
    private final float fixedTimeStep = 1f / 60f;
    private float accumulator = 0f;

    @Override
    public void onUpdate(float deltaTime) {

        accumulator += deltaTime;
        // run fixed-step simulation (with a cap on how many steps per frame implicitly enforced by stepSimulation's maxSubSteps)
        if (accumulator < fixedTimeStep) {
            return;
        }

        // We call stepSimulation with a single step to keep deterministic stepping
        while (accumulator >= fixedTimeStep) {
            dynamicsWorld.stepSimulation(fixedTimeStep, 10);
            accumulator -= fixedTimeStep;
        }
    }

    @Override
    public void onInit() {
        dynamicsWorld.setGravity(DEFAULT_GRAVITY);
        registry.view(PhysicsBody.class, Transform.class)
            .forEach(entity -> {
                var physics = entity.get(PhysicsBody.class);
                synchronizeTransformWithPhysicsSimulation(entity, physics);
                addEntityToPhysicsSimulation(physics);
            });
    }

    private static void synchronizeTransformWithPhysicsSimulation(EntityView entity, PhysicsBody physicsBody) {
        physicsBody.getRigidBody().setMotionState(new EntityMotionState(entity.get(Transform.class)));
    }

    private void addEntityToPhysicsSimulation(PhysicsBody physicsBody) {
        dynamicsWorld.addRigidBody(physicsBody.getRigidBody());
    }

    @Override
    public void onDestroy() {
        System.super.onDestroy();
    }
}
