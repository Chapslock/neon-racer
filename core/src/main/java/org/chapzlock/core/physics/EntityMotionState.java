package org.chapzlock.core.physics;

import javax.vecmath.Quat4f;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;

/**
 * Used to keep Transforms and physics simulation in sync
 */
public class EntityMotionState extends MotionState {
    private final Transform initialTransform = new Transform();
    private final org.chapzlock.core.component.Transform transformComponent;

    public EntityMotionState(org.chapzlock.core.component.Transform tc) {
        this.transformComponent = tc;
        initialTransform.setIdentity();
        Vector3f pos = new Vector3f();
        tc.getPosition(pos);
        initialTransform.origin.set(pos.x, pos.y, pos.z);
        Quaternionf engineQuat = new Quaternionf();
        tc.getRotationQuaternion(engineQuat);
        Quat4f q = PhysicsUtil.toQuat4f(engineQuat);
        initialTransform.setRotation(q);
    }

    @Override
    public Transform getWorldTransform(Transform worldTrans) {
        // Called when Bullet needs the current world transform (usually for initialization)
        worldTrans.set(initialTransform);
        return worldTrans;
    }

    @Override
    public void setWorldTransform(Transform worldTrans) {
        // Called by Bullet each simulation step to update the ECS transform
        transformComponent.setPosition(worldTrans.origin.x, worldTrans.origin.y, worldTrans.origin.z);

        // Convert bullet quaternion to engine quaternion and set it (this marks Euler dirty in Transform)
        Quat4f q = worldTrans.getRotation(new Quat4f());
        transformComponent.setRotationQuaternion(PhysicsUtil.toQuaternionf(q));
    }
}
