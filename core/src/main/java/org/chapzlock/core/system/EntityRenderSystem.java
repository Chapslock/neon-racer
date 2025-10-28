package org.chapzlock.core.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.graphics.PointLight;
import org.chapzlock.core.graphics.material.EntityMaterial;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.registry.ComponentRegistry;
import org.lwjgl.opengl.GL11;

/**
 * The core render system
 */
public class EntityRenderSystem implements System {

    private final ComponentRegistry registry = ComponentRegistry.instance();
    private final MeshSystem meshSystem = MeshSystem.instance();

    private final Map<EntityMaterial, List<EntityView>> renderQueue = new HashMap<>();

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

        renderEntities(camera, light);
    }

    private void renderEntities(Camera camera, PointLight light) {
        registry.view(Mesh.class, EntityMaterial.class, Transform.class)
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
        this.renderQueue.computeIfAbsent(entity.get(EntityMaterial.class), m -> new ArrayList<>()).add(entity);
    }

    private void renderEntitiesFromQueue(Camera camera, PointLight light) {
        for (var batch : renderQueue.entrySet()) {
            EntityMaterial entityMaterial = batch.getKey();
            List<EntityView> renderables = batch.getValue();
            var shader = entityMaterial.getShader();

            // Bind entityMaterial/shader once
            entityMaterial.bind();

            // Load global uniforms once per batch
            shader.loadViewMatrix(camera.getViewMatrix());
            shader.loadProjectionMatrix(camera.getProjectionMatrix());
            if (light != null) {
                shader.loadLight(light);
            }

            for (EntityView entity : renderables) {
                shader.loadTransformationMatrix(entity.get(Transform.class).calculateTransformationMatrix());
                meshSystem.render(entity.get(Mesh.class));
            }

            entityMaterial.unbind();
        }
    }

}
