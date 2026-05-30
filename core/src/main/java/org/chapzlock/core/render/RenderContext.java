package org.chapzlock.core.render;

import java.util.Optional;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityView;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.scene.SceneManager;
import org.joml.Matrix4f;

import lombok.Getter;

/**
 * Per-frame rendering state shared across all passes.
 *
 * <p>Populated once per frame by RenderSystem before passes execute.
 * Passes read from this context rather than querying the registry
 * for camera/light entities directly.</p>
 */
@Getter
public class RenderContext {

    private final ComponentRegistry registry;
    private final MeshSystem meshSystem;
    private final ShaderSystem shaderSystem;
    private final TextureSystem textureSystem;
    private final MaterialSystem materialSystem;
    private final CameraSystem cameraSystem;
    private final SceneManager sceneManager;

    private Camera camera;
    private PointLight pointLight;
    private Sky sky;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private float fogDensity;
    private float fogGradient;

    public RenderContext(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.meshSystem = ctx.getMeshSystem();
        this.shaderSystem = ctx.getShaderSystem();
        this.textureSystem = ctx.getTextureSystem();
        this.materialSystem = ctx.getMaterialSystem();
        this.cameraSystem = ctx.getCameraSystem();
        this.sceneManager = ctx.getSceneManager();
    }

    /**
     * Queries camera, light, and sky entities and caches their data
     * for the current frame. Called once at the start of each render frame.
     *
     * @return true if a camera was found (rendering can proceed), false otherwise
     */
    public boolean beginFrame() {
        Optional<EntityView> cameraView = registry.viewFirst(Camera.class);
        if (cameraView.isEmpty()) {
            Log.error("No Camera entity found. Skipping render.");
            return false;
        }
        this.camera = cameraView.get().get(Camera.class);
        this.projectionMatrix = cameraSystem.calculateProjectionMatrix(camera);
        this.viewMatrix = cameraSystem.calculateViewMatrix(camera);

        this.pointLight = registry.viewFirst(PointLight.class)
            .map(v -> v.get(PointLight.class))
            .orElse(null);

        this.sky = registry.viewFirst(Sky.class)
            .map(v -> v.get(Sky.class))
            .orElse(null);

        if (sceneManager != null && sceneManager.getPrimaryScene() != null) {
            this.fogDensity = sceneManager.getPrimaryScene().getFogDensity();
            this.fogGradient = sceneManager.getPrimaryScene().getFogGradient();
        } else {
            this.fogDensity = 0.007f;
            this.fogGradient = 1.5f;
        }

        return true;
    }
}
