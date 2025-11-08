package org.chapzlock.core.system;

import org.chapzlock.core.application.Application;
import org.chapzlock.core.component.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class CameraSystem {

    private final Matrix4f cameraViewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();

    private float cachedFieldOfView = 0;
    private float cachedNearPlane = 0;
    private float cachedFarPlane = 0;
    private float cachedAspectRatio = 0;

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
        this.cachedAspectRatio = Application.instance().getAppSpec().getWindowSpec().getAspectRatio();
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
            cachedAspectRatio == Application.instance().getAppSpec().getWindowSpec().getAspectRatio();
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
