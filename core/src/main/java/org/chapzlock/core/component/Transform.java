package org.chapzlock.core.component;


import static org.joml.Math.cos;
import static org.joml.Math.sin;
import static org.joml.Math.toRadians;

import org.chapzlock.core.application.Component;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Transform implements Component {
    /**
     * Position in world space
     */
    @Getter
    @Setter
    private Vector3f position = new Vector3f(0, 0,0);
    /**
     * Rotation in degrees
     */
    @Getter
    @Setter
    private Vector3f rotation = new Vector3f(0, 0, 0);
    @Getter
    @Setter
    private float scale = 1;
    /**
     * Stores the transformationMatrix and reuses the same instance across recalculations
     * for better garbage collector performance
     */
    private Matrix4f transformationMatrix = new Matrix4f();

    public Transform(Vector3f position) {
        this.position = position;
    }

    public Transform(Vector3f position, Vector3f rotation) {
        this.position = position;
        this.rotation = rotation;
    }

    public Transform(Vector3f position, Vector3f rotation, float scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    public void setPosition(float x, float y, float z) {
        this.position.x = x;
        this.position.y = y;
        this.position.z = z;
    }

    /**
     * Calculates a transformation matrix for the current transform
     */
    public Matrix4f calculateTransformationMatrix() {
        this.transformationMatrix.identity();
        // Convert rotation from degrees to radians
        float rotX = (float) Math.toRadians(this.rotation.x);
        float rotY = (float) Math.toRadians(this.rotation.y);
        float rotZ = (float) Math.toRadians(this.rotation.z);
        // Apply translation
        this.transformationMatrix.translate(this.position);
        // Apply rotations (order: X → Y → Z)
        this.transformationMatrix
            .rotate(rotX, 1, 0, 0)
            .rotate(rotY, 0, 1, 0)
            .rotate(rotZ, 0, 0, 1);
        // Apply uniform scale
        this.transformationMatrix.scale(this.scale);
        return this.transformationMatrix;
    }

    /**
     * Calculates normalized forward facing vector of the transform
     *
     * @return forward vector
     */
    public Vector3f getForwardVector() {
        float yRotationRad = toRadians(this.rotation.y);
        float xRotationRad = toRadians(this.rotation.x);

        Vector3f front = new Vector3f();
        front.x = cos(yRotationRad) * cos(xRotationRad);
        front.y = sin(xRotationRad);
        front.z = sin(yRotationRad) * cos(xRotationRad);
        return front.normalize();
    }

    /**
     * Returns the upwards facing normalised vector of the transform
     *
     * @return
     */
    public Vector3f getUpVector() {
        float rotationX = toRadians(this.rotation.x);
        float rotationY = toRadians(this.rotation.y);
        float rotationZ = toRadians(this.rotation.z);
        Vector3f up = new Vector3f(0, 1, 0);
        Matrix4f rotationMatrix = new Matrix4f()
            .rotate(rotationX, 1, 0, 0)
            .rotate(rotationY, 0, 1, 0)
            .rotate(rotationZ, 0, 0, 1);
        rotationMatrix.transformDirection(up);
        return up.normalize();
    }
}
