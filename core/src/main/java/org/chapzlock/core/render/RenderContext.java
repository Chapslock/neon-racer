package org.chapzlock.core.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.*;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityView;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.scene.SceneManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
    private Sky sky;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private Vector3f cameraPosition;
    private DirectionalLight directionalLight;
    private List<PointLight> pointLights;
    private Fog fog;
    private AmbientLight ambientLight;
    private boolean warnedNoCamera = false;

    public RenderContext(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.meshSystem = ctx.getMeshSystem();
        this.shaderSystem = ctx.getShaderSystem();
        this.textureSystem = ctx.getTextureSystem();
        this.materialSystem = ctx.getMaterialSystem();
        this.cameraSystem = ctx.getCameraSystem();
        this.sceneManager = ctx.getSceneManager();
    }

    public boolean beginFrame() {
        Optional<EntityView> cameraView = registry.viewFirst(Camera.class);
        if (cameraView.isEmpty()) {
            if (!warnedNoCamera) {
                Log.warn("RenderContext", "No Camera entity found — rendering is skipped. Add a camera to your scene via .camera(x, y, z) on the SceneBuilder.");
                warnedNoCamera = true;
            }
            return false;
        }
        warnedNoCamera = false;
        this.camera = cameraView.get().get(Camera.class);
        this.cameraPosition = camera.getPosition();
        if (cameraSystem != null) {
            this.projectionMatrix = cameraSystem.calculateProjectionMatrix(camera);
            this.viewMatrix = cameraSystem.calculateViewMatrix(camera);
        }

        this.sky = registry.viewFirst(Sky.class)
            .map(v -> v.get(Sky.class))
            .orElse(null);

        var dirLightEntities = registry.view(DirectionalLight.class);
        if (dirLightEntities.size() > 1) {
            Log.warn("RenderContext", "Scene has " + dirLightEntities.size() + " directional lights. Only the first is used.");
        }
        this.directionalLight = registry.viewFirst(DirectionalLight.class)
            .map(v -> v.get(DirectionalLight.class))
            .orElse(null);

        this.pointLights = new ArrayList<>();
        var pointLightEntities = registry.view(PointLight.class);
        if (pointLightEntities.size() > PointLight.MAX_COUNT) {
            Log.warn("RenderContext", "Scene has " + pointLightEntities.size() + " point lights. Only first " +
                PointLight.MAX_COUNT + " are rendered.");
        }
        pointLightEntities.forEach(entity -> {
            if (pointLights.size() < PointLight.MAX_COUNT) {
                pointLights.add(entity.get(PointLight.class));
            }
        });

        this.fog = registry.viewFirst(Fog.class)
            .map(v -> v.get(Fog.class))
            .orElse(null);

        this.ambientLight = registry.viewFirst(AmbientLight.class)
            .map(v -> v.get(AmbientLight.class))
            .orElse(null);

        return true;
    }

    public boolean isFogEnabled() {
        return fog != null;
    }

    public float getEffectiveAmbientStrength() {
        return ambientLight != null ? ambientLight.getStrength() : AmbientLight.DEFAULT_STRENGTH;
    }

}
