package org.chapzlock.core.system;

import static org.lwjgl.opengl.GL11.glClearColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.System;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.graphics.material.MaterialRenderer;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.registry.ComponentRegistry;
import org.lwjgl.opengl.GL11;

/**
 * The core render system
 */
public class RenderSystem implements System {

    private final ComponentRegistry registry = ComponentRegistry.instance();
    private final MeshSystem meshSystem = MeshSystem.instance();
    private final MaterialSystem materialSystem = MaterialSystem.instance();

    private final Map<Material, List<EntityView>> renderQueue = new HashMap<>();

    @Override
    public void onInit() {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
    }

    @Override
    public void onRender(float deltaTime) {
        Camera camera = registry.view(Camera.class)
            .getFirst()
            .get(Camera.class);
        if (camera == null) {
            Log.error("No Camera found! Skipping rendering");
            return;
        }

        PointLight light = getPointLight();
        renderSky();
        renderEntities(camera, light);
    }

    private void renderSky() {
        var skyEntity = registry.view(Sky.class).getFirst();
        if (skyEntity != null) {
            var color = skyEntity.get(Sky.class).getColor();
            glClearColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
    }

    private void renderEntities(Camera camera, PointLight light) {
        registry.view(Mesh.class, Material.class, Transform.class)
            .forEach(this::submitToRenderQueue);
        renderEntitiesFromQueue(camera, light);
        clearRenderQueue();
    }

    private void clearRenderQueue() {
        this.renderQueue.clear();
    }

    private PointLight getPointLight() {
        return registry.view(PointLight.class)
            .getFirst()
            .get(PointLight.class);
    }

    private void submitToRenderQueue(EntityView entity) {
        this.renderQueue.computeIfAbsent(entity.get(Material.class), m -> new ArrayList<>()).add(entity);
    }

    private void renderEntitiesFromQueue(Camera camera, PointLight light) {
        for (var batch : renderQueue.entrySet()) {
            Material material = batch.getKey();
            List<EntityView> renderables = batch.getValue();
            MaterialRenderer materialRenderer = materialSystem.getRenderer(material);
            // Bind material/shader once & Load global uniforms once per batch
            materialRenderer.apply(material, camera, light);

            for (EntityView entity : renderables) {
                materialRenderer.prepareEntity(entity, material);
                meshSystem.render(entity.get(Mesh.class));
            }
            materialRenderer.unapply(material);
        }
    }

}
