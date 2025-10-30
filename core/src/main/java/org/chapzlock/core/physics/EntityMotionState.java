package org.chapzlock.core.physics;

import javax.vecmath.Quat4f;

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
        initialTransform.origin.set(tc.getPosition().x, tc.getPosition().y, tc.getPosition().z);
        initialTransform.setRotation(PhysicsUtil.eulerToQuaternion(tc.getRotation()));
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
        transformComponent.setRotation(PhysicsUtil.quaternionToEuler(worldTrans.getRotation(new Quat4f())));
    }
}
