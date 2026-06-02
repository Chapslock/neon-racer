package org.chapzlock.core.render;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.*;
import org.chapzlock.core.ecs.ComponentRegistry;
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
    private final CameraSystem cameraSystem;
    private final SceneManager sceneManager;

    private Camera camera;
    private Sky sky;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private Vector3f cameraPosition;
    private DirectionalLight directionalLight;
    private List<PointLightInstance> pointLights;
    private Fog fog;
    private AmbientLight ambientLight;
    private boolean warnedNoCamera = false;

    public RenderContext(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.meshSystem = ctx.getMeshSystem();
        this.shaderSystem = ctx.getShaderSystem();
        this.textureSystem = ctx.getTextureSystem();
        this.cameraSystem = ctx.getCameraSystem();
        this.sceneManager = ctx.getSceneManager();
    }

    public boolean beginFrame() {
        var cameraOpt = registry.getSingleton(Camera.class);
        if (cameraOpt.isEmpty()) {
            if (!warnedNoCamera) {
                Log.warn("RenderContext", "No Camera entity found — rendering is skipped. Add a camera to your scene via .camera(x, y, z) on the SceneBuilder.");
                warnedNoCamera = true;
            }
            return false;
        }
        warnedNoCamera = false;
        this.camera = cameraOpt.get();
        this.cameraPosition = camera.getPosition();
        if (cameraSystem != null) {
            this.projectionMatrix = cameraSystem.calculateProjectionMatrix(camera);
            this.viewMatrix = cameraSystem.calculateViewMatrix(camera);
        }

        this.sky = registry.getSingleton(Sky.class).orElse(null);

        int dirLightCount = registry.count(DirectionalLight.class);
        if (dirLightCount > 1) {
            Log.warn("RenderContext", "Scene has " + dirLightCount + " directional lights. Only the first is used.");
        }
        this.directionalLight = registry.getSingleton(DirectionalLight.class).orElse(null);

        this.pointLights = new ArrayList<>();
        int pointLightCount = registry.count(PointLight.class);
        if (pointLightCount > PointLight.MAX_COUNT) {
            Log.warn("RenderContext", "Scene has " + pointLightCount + " point lights. Only first " +
                PointLight.MAX_COUNT + " are rendered.");
        }
        registry.forEach(PointLight.class, Transform.class, (entityId, light, transform) -> {
            if (pointLights.size() < PointLight.MAX_COUNT) {
                Vector3f pos = new Vector3f();
                transform.getPosition(pos);
                pointLights.add(new PointLightInstance(
                    pos, light.getColor(), light.getAttenuation()));
            }
        });

        this.fog = registry.getSingleton(Fog.class).orElse(null);
        this.ambientLight = registry.getSingleton(AmbientLight.class).orElse(null);

        return true;
    }

    public boolean isFogEnabled() {
        return fog != null;
    }

    public float getEffectiveAmbientStrength() {
        return ambientLight != null ? ambientLight.getStrength() : AmbientLight.DEFAULT_STRENGTH;
    }

    /**
     * A resolved point light ready for rendering: position from Transform + properties from PointLight.
     *
     * <p>This combines the entity's world-space position (from its {@link Transform} component)
     * with the light properties (color, attenuation) from its {@link PointLight} component into
     * a single object the shader can consume directly.
     *
     * @param position world-space position of the light (from Transform)
     * @param color light color (from PointLight)
     * @param attenuation distance falloff parameters (from PointLight)
     */
    public record PointLightInstance(Vector3f position, Color color, Attenuation attenuation) {}

}
