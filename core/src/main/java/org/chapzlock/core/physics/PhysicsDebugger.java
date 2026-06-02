package org.chapzlock.core.physics;

import com.bulletphysics.linearmath.IDebugDraw;
import javax.vecmath.Vector3f;

/**
 * TEMPORARY STUB — will be deleted in Task 12 when PhysicsSystem is rewritten.
 * This exists only to allow the codebase to compile while PhysicsWorld is being developed.
 */
@Deprecated
class PhysicsDebugger extends IDebugDraw {
    @Override
    public void drawLine(Vector3f from, Vector3f to, Vector3f color) {
        // no-op stub
    }

    @Override
    public void drawContactPoint(Vector3f PointOnB, Vector3f normalOnB, float distance, int lifeTime, Vector3f color) {
        // no-op stub
    }

    @Override
    public void reportErrorWarning(String warningString) {
        // no-op stub
    }

    @Override
    public void draw3dText(Vector3f location, String textString) {
        // no-op stub
    }

    @Override
    public void setDebugMode(int debugMode) {
        // no-op stub
    }

    @Override
    public int getDebugMode() {
        return 0;
    }
}
