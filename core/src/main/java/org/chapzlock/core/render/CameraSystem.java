package org.chapzlock.core.render;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.window.WindowSpecification;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Computes projection and view matrices from the active camera entity.
 *
 * <p><strong>Projection matrix:</strong> Defines how the 3D world is projected onto the 2D
 * screen using perspective projection. Configured by Camera component (FOV, near/far planes).
 *
 * <p><strong>View matrix:</strong> Positions the camera in the world. Computed from the
 * camera entity's Transform component (position and rotation quaternion).
 *
 * <p><strong>Caching:</strong> The projection matrix only changes when camera settings change
 * (FOV, near/far planes, window resize). The view matrix is recalculated every frame.
 *
 * @see Camera
 * @see Transform
 */
public class CameraSystem {

    private static final Log log = Log.create(CameraSystem.class);

    private final WindowSpecification windowSpec;
    private final Matrix4f cameraViewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();

    private float cachedFieldOfView = 0;
    private float cachedNearPlane = 0;
    private float cachedFarPlane = 0;
    private float cachedAspectRatio = 0;

    private final Vector3f tempPos = new Vector3f();
    private final Vector3f tempForward = new Vector3f();
    private final Vector3f tempCenter = new Vector3f();
    private final Quaternionf tempQuat = new Quaternionf();

    public CameraSystem(WindowSpecification windowSpec) {
        this.windowSpec = windowSpec;
    }

    /**
     * Returns the perspective projection matrix based on the camera settings.
     *
     * @param camera the camera component with FOV, near/far plane settings
     * @return the cached projection matrix (read-only — do not modify)
     */
    public Matrix4f calculateProjectionMatrix(Camera camera) {
        if (camera.getFieldOfView() <= 0 || camera.getFieldOfView() > 180) {
            log.warn("Degenerate FOV=%.1f (expected 0 < fov <= 180 degrees). Projection matrix may be invalid.", camera.getFieldOfView());
        }
        if (windowSpec.getAspectRatio() <= 0) {
            log.warn("Degenerate aspect ratio=%.3f (window may not be initialized yet).", windowSpec.getAspectRatio());
        }
        if (!isCameraSettingsSame(camera)) {
            this.cachedFieldOfView = camera.getFieldOfView();
            this.cachedNearPlane = camera.getNearPlane();
            this.cachedFarPlane = camera.getFarPlane();
            this.cachedAspectRatio = windowSpec.getAspectRatio();
            this.projectionMatrix.identity();
            this.projectionMatrix.perspective(
                (float) Math.toRadians(camera.getFieldOfView()),
                cachedAspectRatio,
                camera.getNearPlane(),
                camera.getFarPlane()
            );
        }
        return projectionMatrix;
    }

    /**
     * Returns the view matrix computed from the camera entity's Transform.
     *
     * <p>Derives the camera's look direction from the Transform's rotation quaternion.
     * The forward vector is computed by rotating (1, 0, 0) by the quaternion — matching
     * the engine's convention where +X is the default forward axis.
     *
     * @param transform the transform component of the camera entity
     * @return the cached view matrix (read-only — do not modify)
     */
    public Matrix4f calculateViewMatrix(Transform transform) {
        transform.getPosition(tempPos);
        transform.getRotationQuaternion(tempQuat);

        tempForward.set(1, 0, 0);
        tempQuat.transform(tempForward);
        tempForward.normalize();

        tempCenter.set(tempPos).add(tempForward);

        this.cameraViewMatrix.identity();
        this.cameraViewMatrix.lookAt(tempPos, tempCenter, new Vector3f(0, 1, 0));
        return cameraViewMatrix;
    }

    private boolean isCameraSettingsSame(Camera camera) {
        return cachedFieldOfView == camera.getFieldOfView() &&
            cachedNearPlane == camera.getNearPlane() &&
            cachedFarPlane == camera.getFarPlane() &&
            cachedAspectRatio == windowSpec.getAspectRatio();
    }
}
