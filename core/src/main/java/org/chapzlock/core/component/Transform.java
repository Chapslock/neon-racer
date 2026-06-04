package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Position, rotation, and scale of an entity in local space, plus its computed world matrix.
 *
 * <p>Local-space data (position, rotation, scale) is set by game code. The world matrix
 * is computed by {@link org.chapzlock.core.ecs.TransformSystem} each frame for entities
 * in a hierarchy. For standalone entities (no hierarchy), {@link #getWorldMatrix()} returns
 * the local matrix directly with zero overhead.
 *
 * <p>Rotation is stored in both Euler angles (degrees, for gameplay code) and quaternion
 * (for engine/physics). Synchronization is lazy — converting only when the other
 * representation is accessed.
 */
public class Transform implements Component {

    private final Vector3f position = new Vector3f(0, 0, 0);
    private final Vector3f scale = new Vector3f(1, 1, 1);
    private final Vector3f rotationEuler = new Vector3f(0, 0, 0);
    private final Quaternionf rotationQuat = new Quaternionf().identity();

    private final Matrix4f localMatrix = new Matrix4f();
    private final Matrix4f worldMatrix = new Matrix4f();

    private boolean localDirty = true;
    private boolean quatDirty = false;
    private boolean eulerDirty = false;
    private boolean inHierarchy = false;

    public Transform() {}

    public Transform(Vector3f position) {
        this.position.set(position);
    }

    public Transform(Vector3f position, Vector3f rotationEulerDeg) {
        this.position.set(position);
        this.rotationEuler.set(rotationEulerDeg);
        quatFromEuler();
    }

    public Transform(Vector3f position, Vector3f rotationEulerDeg, float uniformScale) {
        this.position.set(position);
        this.rotationEuler.set(rotationEulerDeg);
        this.scale.set(uniformScale, uniformScale, uniformScale);
        quatFromEuler();
    }

    public Transform(Vector3f position, Vector3f rotationEulerDeg, Vector3f scale) {
        this.position.set(position);
        this.rotationEuler.set(rotationEulerDeg);
        this.scale.set(scale);
        quatFromEuler();
    }

    // --- Factory methods ---

    public static Transform at(float x, float y, float z) {
        return new Transform(new Vector3f(x, y, z));
    }

    public static Transform at(float x, float y, float z, float rotX, float rotY, float rotZ) {
        return new Transform(new Vector3f(x, y, z), new Vector3f(rotX, rotY, rotZ));
    }

    public static Transform at(float x, float y, float z, float rotX, float rotY, float rotZ, float uniformScale) {
        return new Transform(new Vector3f(x, y, z), new Vector3f(rotX, rotY, rotZ), uniformScale);
    }

    public static Transform at(float x, float y, float z, float rotX, float rotY, float rotZ, float sx, float sy, float sz) {
        return new Transform(new Vector3f(x, y, z), new Vector3f(rotX, rotY, rotZ), new Vector3f(sx, sy, sz));
    }

    // --- Position ---

    public void getPosition(Vector3f dest) {
        dest.set(position);
    }

    public float getX() { return position.x; }
    public float getY() { return position.y; }
    public float getZ() { return position.z; }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        localDirty = true;
    }

    public void setPosition(Vector3f pos) {
        position.set(pos);
        localDirty = true;
    }

    // --- Scale (non-uniform) ---

    public void getScale(Vector3f dest) {
        dest.set(scale);
    }

    public void setScale(float x, float y, float z) {
        scale.set(x, y, z);
        localDirty = true;
    }

    public void setScale(float uniform) {
        scale.set(uniform, uniform, uniform);
        localDirty = true;
    }

    // --- Rotation: Euler (degrees) ---

    public void getRotationEuler(Vector3f dest) {
        if (eulerDirty) {
            eulerFromQuat();
        }
        dest.set(rotationEuler);
    }

    public void setRotationEuler(float xDeg, float yDeg, float zDeg) {
        rotationEuler.set(xDeg, yDeg, zDeg);
        quatDirty = true;
        eulerDirty = false;
        localDirty = true;
    }

    // --- Rotation: Quaternion ---

    public void getRotationQuaternion(Quaternionf dest) {
        if (quatDirty) {
            quatFromEuler();
        }
        dest.set(rotationQuat);
    }

    public void setRotationQuaternion(Quaternionf q) {
        rotationQuat.set(q);
        eulerDirty = true;
        quatDirty = false;
        localDirty = true;
    }

    /**
     * Points this transform toward the given world-space target.
     *
     * <p>Computes a rotation quaternion that maps the engine's forward axis (+X)
     * onto the direction from this position to the target. Uses yaw (Y-axis rotation)
     * and pitch (elevation) decomposition to avoid roll.
     *
     * <p>If the target equals the current position, does nothing.
     *
     * @param target the world-space position to look at
     */
    public void lookAt(Vector3f target) {
        Vector3f dir = new Vector3f(target).sub(position);
        float len2 = dir.lengthSquared();
        if (len2 == 0f) {
            return;
        }
        dir.normalize();

        float yawRad = (float) Math.atan2(-dir.z, dir.x);
        float pitchRad = (float) Math.asin(Math.max(-1f, Math.min(1f, dir.y)));

        rotationQuat.identity().rotateY(yawRad).rotateZ(pitchRad);
        eulerDirty = true;
        quatDirty = false;
        localDirty = true;
    }

    // --- Matrices ---

    public Matrix4f getLocalMatrix() {
        if (localDirty) {
            recomputeLocalMatrix();
        }
        return localMatrix;
    }

    public Matrix4f getWorldMatrix() {
        if (!inHierarchy) {
            return getLocalMatrix();
        }
        return worldMatrix;
    }

    // --- Hierarchy flag (set by TransformSystem) ---

    public boolean isInHierarchy() {
        return inHierarchy;
    }

    public void setInHierarchy(boolean inHierarchy) {
        this.inHierarchy = inHierarchy;
    }

    // --- Deprecated shims (removed in subsequent tasks) ---

    // --- Internal ---

    private void recomputeLocalMatrix() {
        if (quatDirty) {
            quatFromEuler();
        }
        localMatrix.identity()
            .translate(position)
            .rotate(rotationQuat)
            .scale(scale);
        localDirty = false;
    }

    private void quatFromEuler() {
        float rx = (float) Math.toRadians(rotationEuler.x);
        float ry = (float) Math.toRadians(rotationEuler.y);
        float rz = (float) Math.toRadians(rotationEuler.z);
        rotationQuat.identity().rotateX(rx).rotateY(ry).rotateZ(rz);
        quatDirty = false;
    }

    private void eulerFromQuat() {
        float x = rotationQuat.x, y = rotationQuat.y, z = rotationQuat.z, w = rotationQuat.w;
        double t0 = 2.0 * (w * x + y * z);
        double t1 = 1.0 - 2.0 * (x * x + y * y);
        double rx = Math.atan2(t0, t1);

        double t2 = Math.max(-1.0, Math.min(1.0, 2.0 * (w * y - z * x)));
        double ry = Math.asin(t2);

        double t3 = 2.0 * (w * z + x * y);
        double t4 = 1.0 - 2.0 * (y * y + z * z);
        double rz = Math.atan2(t3, t4);

        rotationEuler.set(
            (float) Math.toDegrees(rx),
            (float) Math.toDegrees(ry),
            (float) Math.toDegrees(rz)
        );
        eulerDirty = false;
    }
}
