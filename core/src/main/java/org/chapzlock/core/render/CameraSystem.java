package org.chapzlock.core.render;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.window.WindowSpecification;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Calculates projection and view matrices for the camera.
 *
 * <p>Caches matrices to avoid recalculation when camera settings haven't changed.
 */
public class CameraSystem {

    private final WindowSpecification windowSpec;
    private final Matrix4f cameraViewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();

    private float cachedFieldOfView = 0;
    private float cachedNearPlane = 0;
    private float cachedFarPlane = 0;
    private float cachedAspectRatio = 0;

    public CameraSystem(WindowSpecification windowSpec) {
        this.windowSpec = windowSpec;
    }

    /**
     * Calculates the perspective projection matrix based on the camera settings
     *
     * @param camera
     * @return perspective projection matrix
     */
    public Matrix4f calculateProjectionMatrix(Camera camera) {
        if (isCameraSettingsSame(camera)) {
            return new Matrix4f(projectionMatrix);
        }
        this.cachedFieldOfView = camera.getFieldOfView();
        this.cachedNearPlane = camera.getNearPlane();
        this.cachedFarPlane = camera.getFarPlane();
        this.cachedAspectRatio = windowSpec.getAspectRatio();
        this.projectionMatrix.identity();
        this.projectionMatrix.perspective(
            camera.getFieldOfView(),
            cachedAspectRatio,
            camera.getNearPlane(),
            camera.getFarPlane()
        );
        return new Matrix4f(this.projectionMatrix);
    }

    private boolean isCameraSettingsSame(Camera camera) {
        return cachedFieldOfView == camera.getFieldOfView() &&
            cachedNearPlane == camera.getNearPlane() &&
            cachedFarPlane == camera.getFarPlane() &&
            cachedAspectRatio == windowSpec.getAspectRatio();
    }

    public Matrix4f calculateViewMatrix(Camera camera) {
        this.cameraViewMatrix.identity();
        this.cameraViewMatrix.lookAt(
            camera.getPosition(),
            calculateCameraCenter(camera),
            camera.getCameraUp()
        );
        return new Matrix4f(this.cameraViewMatrix);
    }

    private Vector3fc calculateCameraCenter(Camera camera) {
        return camera.getPosition().add(camera.getCameraFront(), new Vector3f());
    }
}
