package org.chapzlock.core.component;


import org.chapzlock.core.application.Component;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * Transform that keeps both Euler angles (degrees) and a quaternion in sync.
 * <p>
 * - Euler (degrees) is the convenience API for gameplay code.
 * - Quaternion is used internally by engine/physics for stable rotations.
 * <p>
 * Synchronization is lazy: changing one representation marks the other dirty and
 * converts only when that representation is read.
 */
@Getter
@Setter
@NoArgsConstructor
public class Transform implements Component {
    /**
     * position in world space
     */
    private final Vector3f position = new Vector3f(0, 0, 0);

    /**
     * Euler rotation stored in degrees (order: X -> Y -> Z)
     * This is what gameplay code will typically use via set/getRotationEuler(...)
     */
    private final Vector3f rotationEuler = new Vector3f(0, 0, 0);

    /**
     * Quaternion representation (JOML) used for math/physics
     */
    private final Quaternionf rotationQuat = new Quaternionf().identity();
    /**
     * Cached transformation matrix reused to avoid allocations
     */
    private final Matrix4f transformationMatrix = new Matrix4f();
    private final Vector3f tmpVec = new Vector3f();
    /**
     * Uniform scale
     */
    private float scale = 1.0f;
    /* Dirty flags to avoid redundant conversions / matrix recalculation */
    private boolean matrixDirty = true;
    private boolean quatDirty = false;   // true: quaternion is out of date with rotationEuler
    private boolean eulerDirty = false;  // true: euler is out of date with rotationQuat

    /* --- constructors --- */
    public Transform(Vector3f position) {
        this.position.set(position);
        markMatrixDirty();
    }

    /* --- helpers to mark dirty --- */
    private void markMatrixDirty() {
        this.matrixDirty = true;
    }

    public Transform(Vector3f position, Vector3f rotationEuler) {
        this.position.set(position);
        this.rotationEuler.set(rotationEuler);
        this.quatFromEuler(); // build quaternion from euler immediately
        markMatrixDirty();
    }

    /**
     * Convert rotationEuler (degrees) -> rotationQuat (radians, order X then Y then Z)
     */
    private void quatFromEuler() {
        // Convert degrees to radians
        float rx = (float) Math.toRadians(this.rotationEuler.x);
        float ry = (float) Math.toRadians(this.rotationEuler.y);
        float rz = (float) Math.toRadians(this.rotationEuler.z);

        // Build quaternion with the same rotation order used in earlier code (X -> Y -> Z)
        // We reset to identity then rotate X then Y then Z to match matrix.rotateX().rotateY().rotateZ()
        this.rotationQuat.identity()
            .rotateX(rx)
            .rotateY(ry)
            .rotateZ(rz);

        this.quatDirty = false;
        this.eulerDirty = false;
    }

    public Transform(Vector3f position, Vector3f rotationEuler, float scale) {
        this.position.set(position);
        this.rotationEuler.set(rotationEuler);
        this.scale = scale;
        this.quatFromEuler();
        markMatrixDirty();
    }

    private void markQuatDirty() {
        this.quatDirty = true;
        this.eulerDirty = false; // we consider euler authoritative until quaternion read
    }

    private void markEulerDirty() {
        this.eulerDirty = true;
        this.quatDirty = false;  // quaternion authoritative until euler read
    }

    /* --- Euler rotation API (degrees) --- */

    /* --- position setters --- */
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        markMatrixDirty();
    }

    public void setPosition(Vector3f newPos) {
        this.position.set(newPos);
        markMatrixDirty();
    }

    /**
     * Set rotation using Euler angles in degrees (X, Y, Z). This will mark the quaternion
     * out of date and only update it when the quaternion is requested.
     */
    public void setRotationEuler(float xDeg, float yDeg, float zDeg) {
        this.rotationEuler.set(xDeg, yDeg, zDeg);
        this.quatDirty = true;
        this.eulerDirty = false;
        markMatrixDirty();
    }

    /* --- Quaternion API --- */

    /**
     * Get Euler angles in degrees. If the euler vector is stale (because quaternion was changed),
     * convert lazily.
     */
    public Vector3f getRotationEuler() {
        if (this.eulerDirty) {
            this.eulerFromQuat();
        }
        return this.rotationEuler;
    }

    public void setRotationEuler(Vector3f eulerDegrees) {
        this.rotationEuler.set(eulerDegrees);
        this.quatDirty = true;
        this.eulerDirty = false;
        markMatrixDirty();
    }

    /* --- conversions (lazy) --- */

    /**
     * Convert rotationQuat -> rotationEuler (degrees), using Tait-Bryan angles X (pitch), Y (yaw), Z (roll)
     * Formula:
     * X = atan2(2*(w*x + y*z), 1 - 2*(x*x + y*y))
     * Y = asin( clamp(2*(w*y - z*x), -1, 1) )
     * Z = atan2(2*(w*z + x*y), 1 - 2*(y*y + z*z))
     * <p>
     * This produces the Euler angles in radians for order X->Y->Z and we convert to degrees.
     */
    private void eulerFromQuat() {
        float x = rotationQuat.x;
        float y = rotationQuat.y;
        float z = rotationQuat.z;
        float w = rotationQuat.w;

        double t0 = 2.0 * (w * x + y * z);
        double t1 = 1.0 - 2.0 * (x * x + y * y);
        double rx = Math.atan2(t0, t1);

        double t2 = 2.0 * (w * y - z * x);
        t2 = Math.max(-1.0, Math.min(1.0, t2));
        double ry = Math.asin(t2);

        double t3 = 2.0 * (w * z + x * y);
        double t4 = 1.0 - 2.0 * (y * y + z * z);
        double rz = Math.atan2(t3, t4);

        // convert to degrees and store
        this.rotationEuler.set(
            (float) Math.toDegrees(rx),
            (float) Math.toDegrees(ry),
            (float) Math.toDegrees(rz)
        );

        this.eulerDirty = false;
        this.quatDirty = false;
    }

    /**
     * Get the rotation quaternion. If quaternion is stale (because Euler was changed),
     * convert lazily.
     */
    public Quaternionf getRotationQuaternion() {
        if (this.quatDirty) {
            this.quatFromEuler();
        }
        return this.rotationQuat;
    }

    /* --- transformation matrix (cached) --- */

    /**
     * Set rotation using a JOML Quaternionf (assumed normalized). Marks Euler dirty.
     */
    public void setRotationQuaternion(Quaternionf q) {
        this.rotationQuat.set(q);
        this.eulerDirty = true;
        this.quatDirty = false;
        markMatrixDirty();
    }

    /* --- direction helpers (re-use vectors to avoid allocations) --- */

    /**
     * Returns cached transformation matrix. Recomputes only when necessary.
     * Order is: translate -> rotate (via quaternion) -> scale.
     */
    public Matrix4f getTransformationMatrix() {
        if (this.matrixDirty) {
            // Start fresh
            this.transformationMatrix.identity();

            // translate
            this.transformationMatrix.translate(this.position);

            // ensure quaternion up-to-date
            if (this.quatDirty) {
                this.quatFromEuler();
            }
            this.transformationMatrix.rotate(this.rotationQuat);

            // scale
            this.transformationMatrix.scale(this.scale);

            this.matrixDirty = false;
        }
        return this.transformationMatrix;
    }

    /**
     * Forward vector in world space (canonical forward is +Z).
     */
    public Vector3f getForwardVector() {
        // canonical forward is (0,0,1)
        tmpVec.set(0, 0, 1);

        if (this.quatDirty) {
            this.quatFromEuler();
        }
        // rotate forward by quaternion in-place (Quaternionf.transform(Vector3f))
        this.rotationQuat.transform(tmpVec);
        return tmpVec.normalize();
    }

    /**
     * Right vector in world space (canonical right is +X).
     */
    public Vector3f getRightVector() {
        tmpVec.set(1, 0, 0);
        if (this.quatDirty) {
            this.quatFromEuler();
        }
        this.rotationQuat.transform(tmpVec);
        return tmpVec.normalize();
    }
}
