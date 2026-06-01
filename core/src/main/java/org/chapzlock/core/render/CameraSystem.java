package org.chapzlock.core.render;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.window.WindowSpecification;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Computes projection and view matrices from the active camera entity.
 *
 * <p><strong>What is a projection matrix?</strong> It defines how the 3D world is projected
 * onto the 2D screen. A perspective projection makes distant objects smaller (like how your
 * eyes work). It's defined by field-of-view (how wide the camera sees), aspect ratio (screen
 * width/height), and near/far planes (minimum and maximum visible distance).
 *
 * <p><strong>What is a view matrix?</strong> It positions the camera in the world. Technically,
 * it transforms world coordinates into "camera space" — as if the camera were at the origin
 * looking down -Z. This is computed from the camera's position and its look-at direction.
 *
 * <p><strong>Why these matrices matter:</strong> Every vertex rendered goes through:
 * {@code screenPosition = projectionMatrix × viewMatrix × modelMatrix × vertexPosition}.
 * The model matrix positions the object in the world, the view matrix adjusts for where
 * the camera is, and the projection matrix applies perspective.
 *
 * <p><strong>Caching:</strong> The projection matrix only changes when camera settings change
 * (FOV, near/far planes, window resize). The view matrix changes every frame if the camera
 * moves. This system caches the projection matrix and only recalculates when settings differ.
 *
 * @see org.chapzlock.core.component.Camera
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
     * Returns the perspective projection matrix based on the camera settings.
     *
     * <p>The returned matrix is owned by this system — do not mutate it. It is recalculated
     * only when camera settings change (FOV, near/far planes, window resize).
     *
     * @param camera the camera component with FOV, near/far plane settings
     * @return the cached projection matrix (read-only — do not modify)
     */
    public Matrix4f calculateProjectionMatrix(Camera camera) {
        if (camera.getFieldOfView() <= 0 || camera.getFieldOfView() > Math.PI) {
            Log.warn("CameraSystem: degenerate FOV=" + camera.getFieldOfView() + " (expected 0 < fov < π radians). Projection matrix may be invalid.");
        }
        if (windowSpec.getAspectRatio() <= 0) {
            Log.warn("CameraSystem: degenerate aspect ratio=" + windowSpec.getAspectRatio() + " (window may not be initialized yet).");
        }
        if (!isCameraSettingsSame(camera)) {
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
        }
        return projectionMatrix;
    }

    private boolean isCameraSettingsSame(Camera camera) {
        return cachedFieldOfView == camera.getFieldOfView() &&
            cachedNearPlane == camera.getNearPlane() &&
            cachedFarPlane == camera.getFarPlane() &&
            cachedAspectRatio == windowSpec.getAspectRatio();
    }

    /**
     * Returns the view matrix based on the camera's position and direction.
     *
     * <p>The returned matrix is owned by this system — do not mutate it. It is
     * recalculated every frame (camera movement is expected to be frequent).
     *
     * @param camera the camera component with position and direction
     * @return the cached view matrix (read-only — do not modify)
     */
    public Matrix4f calculateViewMatrix(Camera camera) {
        this.cameraViewMatrix.identity();
        this.cameraViewMatrix.lookAt(
            camera.getPosition(),
            calculateCameraCenter(camera),
            camera.getCameraUp()
        );
        return cameraViewMatrix;
    }

    private Vector3fc calculateCameraCenter(Camera camera) {
        return camera.getPosition().add(camera.getCameraFront(), new Vector3f());
    }
}
