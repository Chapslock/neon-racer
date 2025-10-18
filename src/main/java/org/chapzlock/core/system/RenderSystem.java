package org.chapzlock.core.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.graphics.Camera;
import org.chapzlock.core.graphics.Material;
import org.chapzlock.core.graphics.Mesh;
import org.chapzlock.core.graphics.PointLight;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.registry.ComponentRegistry;
import org.lwjgl.opengl.GL11;

import lombok.RequiredArgsConstructor;

/**
 * The core render system
 */
@RequiredArgsConstructor
public class RenderSystem implements System {

    private final ComponentRegistry registry;

    private final Map<Material, List<EntityView>> renderQueue = new HashMap<>();

    @Override
    public void onInit() {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
    }

    @Override
    public void onRender(float deltaTime) {
        clearRenderQueue();

        Camera camera = registry.view(Camera.class)
            .getFirst()
            .get(Camera.class);
        if (camera == null) {
            Log.error("No Camera found! Skipping rendering");
            return;
        }

        PointLight light = getPointLight();

        registry.view(Mesh.class, Material.class, Transform.class)
            .forEach(this::submitToRenderQueue);

        renderEntitiesFromQueue(camera, light);
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
            var shader = material.getShader();

            // Bind material/shader once
            material.bind();

            // Load global uniforms once per batch
            shader.loadViewMatrix(camera.getViewMatrix());
            shader.loadProjectionMatrix(camera.getProjectionMatrix());
            if (light != null) {
                shader.loadLight(light);
            }

            for (EntityView entity : renderables) {
                shader.loadTransformationMatrix(entity.get(Transform.class).calculateTransformationMatrix());
                entity.get(Mesh.class).render();
            }

            material.unbind();
        }
    }

}
