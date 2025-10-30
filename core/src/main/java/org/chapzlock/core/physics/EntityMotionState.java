package org.chapzlock.core.physics;

import javax.vecmath.Quat4f;

import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;

/**
 * Used to keep Transforms and physics simulation in sync
 */
public class EntityMotionState extends MotionState {
    private final Transform initialTransformation = new Transform();
    private final org.chapzlock.core.component.Transform transformComponent;


    public EntityMotionState(org.chapzlock.core.component.Transform tc) {
        this.transformComponent = tc;
        initialTransformation.setIdentity();
        initialTransformation.origin.set(transformComponent.getPosition().x, transformComponent.getPosition().y, transformComponent.getPosition().z);
        initialTransformation.setRotation(PhysicsUtil.eulerToQuaternion(transformComponent.getRotation()));
    }


    @Override
    public Transform getWorldTransform(Transform worldTrans) {
        // Called by JBullet when it needs the initial transform for the body (or when saving)
        worldTrans.setIdentity();
        worldTrans.origin.set(transformComponent.getPosition().x, transformComponent.getPosition().y, transformComponent.getPosition().z);
        worldTrans.setRotation(PhysicsUtil.eulerToQuaternion(transformComponent.getRotation()));
        return worldTrans;
    }


    @Override
    public void setWorldTransform(Transform worldTrans) {
        // Called by JBullet after physics steps to inform us of the new transform.
        transformComponent.setPosition(worldTrans.origin.x, worldTrans.origin.y, worldTrans.origin.z);
        // copy rotation quaternion
        transformComponent.setRotation(PhysicsUtil.quaternionToEuler(worldTrans.getRotation(new Quat4f())));
        ;
    }
}
