package org.chapzlock.core.render;

import static org.lwjgl.opengl.GL11.glClearColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.EntityView;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.lwjgl.opengl.GL11;

/**
 * The core render system
 */
public class RenderSystem implements GameSystem {

    private ComponentRegistry registry;
    private MeshSystem meshSystem;
    private MaterialSystem materialSystem;

    private final Map<Material, List<EntityView>> renderQueue = new HashMap<>();

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.meshSystem = ctx.getMeshSystem();
        this.materialSystem = ctx.getMaterialSystem();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
    }

    @Override
    public void onRender(float deltaTime) {
        Optional<EntityView> cameraView = registry.viewFirst(Camera.class);
        if (cameraView.isEmpty()) {
            Log.error("No Camera entity found. Skipping render.");
            return;
        }
        Camera camera = cameraView.get().get(Camera.class);

        PointLight light = registry.viewFirst(PointLight.class)
            .map(v -> v.get(PointLight.class))
            .orElse(null);

        Sky sky = registry.viewFirst(Sky.class)
            .map(v -> v.get(Sky.class))
            .orElse(null);

        renderEntities(camera, light, sky);
    }

    private void renderEntities(Camera camera, PointLight light, Sky sky) {
        registry.view(Mesh.class, Material.class, Transform.class)
            .forEach(this::submitToRenderQueue);
        renderSky(sky);
        renderEntitiesFromQueue(camera, light, sky);
        clearRenderQueue();
    }

    private void renderSky(Sky sky) {
        if (sky != null) {
            Color color = sky.getColor();
            glClearColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
    }

    private void clearRenderQueue() {
        this.renderQueue.clear();
    }

    private void submitToRenderQueue(EntityView entity) {
        this.renderQueue.computeIfAbsent(entity.get(Material.class), m -> new ArrayList<>()).add(entity);
    }

    private void renderEntitiesFromQueue(Camera camera, PointLight light, Sky sky) {
        for (var batch : renderQueue.entrySet()) {
            Material material = batch.getKey();
            List<EntityView> renderables = batch.getValue();
            MaterialRenderer materialRenderer = materialSystem.getRenderer(material);
            // Bind material/shader once & Load global uniforms once per batch
            materialRenderer.apply(material, camera, light, sky);

            for (EntityView entity : renderables) {
                materialRenderer.prepareEntity(entity, material);
                meshSystem.render(entity.get(Mesh.class));
            }
            materialRenderer.unapply(material);
        }
    }

}
