package org.chapzlock.core.physics;

import javax.vecmath.Vector3f;

import org.chapzlock.core.logging.Log;

import com.bulletphysics.linearmath.DebugDrawModes;
import com.bulletphysics.linearmath.IDebugDraw;

/**
 * Debugger for the physics simulation.
 * Used for drawing wireframes and physics simulation details onto the screen.
 */
public class PhysicsDebugger extends IDebugDraw {

    private int debugMode = DebugDrawModes.DRAW_WIREFRAME | DebugDrawModes.DRAW_CONTACT_POINTS | DebugDrawModes.DRAW_AABB;

    @Override
    public void drawLine(Vector3f from, Vector3f to, Vector3f color) {
        PhysicsDebugRenderer.addLineToDrawQueue(
            new org.joml.Vector3f(from.x, from.y, from.z),
            new org.joml.Vector3f(to.x, to.y, to.z),
            new org.joml.Vector3f(color.x, color.y, color.z)
        );
    }

    @Override
    public void drawContactPoint(Vector3f pointOnB, Vector3f normalOnB, float distance, int lifeTime, Vector3f color) {
        Vector3f to = new Vector3f(
            pointOnB.x + normalOnB.x * distance,
            pointOnB.y + normalOnB.y * distance,
            pointOnB.z + normalOnB.z * distance
        );
        drawLine(pointOnB, to, color);
    }

    @Override
    public void reportErrorWarning(String warningString) {
        Log.error("[Physics Warning] " + warningString);
    }

    @Override
    public void draw3dText(Vector3f location, String textString) {

    }

    @Override
    public void setDebugMode(int debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public int getDebugMode() {
        return debugMode;
    }
}
