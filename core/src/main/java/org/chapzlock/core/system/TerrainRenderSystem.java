package org.chapzlock.core.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.asset.ResourceManager;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.graphics.PointLight;
import org.chapzlock.core.graphics.material.TerrainMaterial;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.registry.ComponentRegistry;
import org.lwjgl.opengl.GL11;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import lombok.RequiredArgsConstructor;

/**
 * The core render system
 */
@RequiredArgsConstructor
public class TerrainRenderSystem implements System {

    private final ComponentRegistry registry;
    private final ResourceManager resourceManager = ResourceManager.instance();

    private final Map<TerrainMaterial, List<EntityView>> renderQueue = new Object2ObjectArrayMap<>();

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

        registry.view(Mesh.class, TerrainMaterial.class, Transform.class)
            .forEach(this::submitToRenderQueue);
        renderEntitiesFromQueue(camera, light);
        clearRenderQueue();
    }

    @Override
    public void onInit() {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
    }

    private PointLight getPointLight() {
        return registry.view(PointLight.class)
            .getFirst()
            .get(PointLight.class);
    }

    private void submitToRenderQueue(EntityView entity) {
        this.renderQueue.computeIfAbsent(entity.get(TerrainMaterial.class), m -> new ArrayList<>()).add(entity);
    }

    private void renderEntitiesFromQueue(Camera camera, PointLight light) {
        for (var batch : renderQueue.entrySet()) {
            TerrainMaterial entityMaterial = batch.getKey();
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
                resourceManager.renderMesh(entity.get(Mesh.class).getMeshHandle());
            }

            entityMaterial.unbind();
        }
    }

    private void clearRenderQueue() {
        this.renderQueue.clear();
    }

}
