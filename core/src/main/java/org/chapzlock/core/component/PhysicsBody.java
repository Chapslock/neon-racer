package org.chapzlock.core.component;

import javax.vecmath.Vector3f;

import org.chapzlock.core.application.Component;
import org.chapzlock.core.physics.PhysicsSpecs;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;

import lombok.Getter;

/**
 * This component makes the entity eligible for physics simulation.
 */
@Getter
public class PhysicsBody implements Component {
    private static final Vector3f INITIAL_LOCAL_INERTIA = new Vector3f(0f, 0f, 0f);
    private RigidBody rigidBody;
    private CollisionShape collisionShape;
    private float mass;

    public PhysicsBody(CollisionShape shape, PhysicsSpecs specs) {
        this.collisionShape = shape;
        this.mass = specs.getMass();
        if (specs.getMass() != 0) {
            shape.calculateLocalInertia(specs.getMass(), INITIAL_LOCAL_INERTIA);
        }
        RigidBodyConstructionInfo rigidBodyConstructionInfo = new RigidBodyConstructionInfo(
            specs.getMass(),
            null,
            shape,
            INITIAL_LOCAL_INERTIA
        );
        this.rigidBody = new RigidBody(rigidBodyConstructionInfo);
        rigidBody.setFriction(specs.getFriction());
        rigidBody.setRestitution(specs.getRestitution());
        rigidBody.setDamping(specs.getLinearDamping(), specs.getAngularDamping());
    }
}
