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

    private static final Log log = Log.create(RenderContext.class);

    private final ComponentRegistry registry;
    private final MeshRegistry meshRegistry;
    private final ShaderRegistry shaderRegistry;
    private final TextureRegistry textureRegistry;
    private final CameraSystem cameraSystem;
    private final SceneManager sceneManager;
    private final Vector3f[] pointLightPositionPool = new Vector3f[PointLight.MAX_COUNT];

    private Camera camera;
    private Transform cameraTransform;
    private Sky sky;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private Vector3f cameraPosition;
    private DirectionalLight directionalLight;
    private List<PointLightInstance> pointLights;
    private Fog fog;
    private AmbientLight ambientLight;
    private boolean warnedNoCamera = false;
    private boolean warnedMeshWithoutMaterial = false;

    public RenderContext(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.meshRegistry = ctx.getMeshRegistry();
        this.shaderRegistry = ctx.getShaderRegistry();
        this.textureRegistry = ctx.getTextureRegistry();
        this.cameraSystem = ctx.getCameraSystem();
        this.sceneManager = ctx.getSceneManager();
        for (int i = 0; i < pointLightPositionPool.length; i++) {
            pointLightPositionPool[i] = new Vector3f();
        }
    }

    public boolean beginFrame() {
        final Camera[] foundCamera = {null};
        final Transform[] foundTransform = {null};
        registry.forEach(Camera.class, Transform.class, (entityId, camera, transform) -> {
            if (foundCamera[0] == null) {
                foundCamera[0] = camera;
                foundTransform[0] = transform;
            }
        });

        if (foundCamera[0] == null) {
            if (!warnedNoCamera) {
                log.warn("No entity with Camera + Transform found — rendering is skipped. Add .transform(...).component(new Camera()) to your scene.");
                warnedNoCamera = true;
            }
            return false;
        }
        warnedNoCamera = false;
        this.camera = foundCamera[0];
        this.cameraTransform = foundTransform[0];

        Vector3f pos = new Vector3f();
        cameraTransform.getPosition(pos);
        this.cameraPosition = pos;

        if (cameraSystem != null) {
            this.projectionMatrix = cameraSystem.calculateProjectionMatrix(camera);
            this.viewMatrix = cameraSystem.calculateViewMatrix(cameraTransform);
        }

        this.sky = registry.getSingleton(Sky.class).orElse(null);

        int dirLightCount = registry.count(DirectionalLight.class);
        if (dirLightCount > 1) {
            log.warn("Scene has %d directional lights. Only the first is used.", dirLightCount);
        }
        this.directionalLight = registry.getSingleton(DirectionalLight.class).orElse(null);

        this.pointLights = new ArrayList<>();
        int pointLightCount = registry.count(PointLight.class);
        if (pointLightCount > PointLight.MAX_COUNT) {
            log.warn("Scene has %d point lights. Only first %d are rendered.", pointLightCount, PointLight.MAX_COUNT);
        }
        final int[] index = {0};
        registry.forEach(PointLight.class, Transform.class, (entityId, light, transform) -> {
            if (index[0] < PointLight.MAX_COUNT) {
                transform.getPosition(pointLightPositionPool[index[0]]);
                pointLights.add(new PointLightInstance(
                    pointLightPositionPool[index[0]], light.getColor(), light.getAttenuation()));
                index[0]++;
            }
        });

        this.fog = registry.getSingleton(Fog.class).orElse(null);
        this.ambientLight = registry.getSingleton(AmbientLight.class).orElse(null);

        if (!warnedMeshWithoutMaterial) {
            registry.forEach(Mesh.class, Transform.class, (entityId, mesh, transform) -> {
                if (registry.getComponent(entityId, PbrMaterial.class) == null &&
                    registry.getComponent(entityId, SpriteMaterial.class) == null) {
                    Name nameComp = registry.getComponent(entityId, Name.class);
                    String entityName = nameComp != null ? nameComp.getValue() : "entity " + entityId;
                    log.warn("Entity \"%s\" has Mesh but no material — will not render", entityName);
                    warnedMeshWithoutMaterial = true;
                }
            });
        }

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
