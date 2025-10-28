package org.chapzlock.core.system;

import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_LIGHT_COLOR;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_LIGHT_POSITION;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_PROJECTION_MATRIX;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_TRANSFORMATION_MATRIX;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_VIEW_MATRIX;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.System;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.registry.ComponentRegistry;
import org.lwjgl.opengl.GL11;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

/**
 * Terrain rendering system.
 * Renders all entities with the terrain component attached
 */
public class TerrainRenderSystem implements System {

    private final ComponentRegistry registry = ComponentRegistry.instance();
    private final MeshSystem meshSystem = MeshSystem.instance();
    private final ShaderSystem shaderSystem = new ShaderSystem();
    private final TerrainMaterialSystem materialSystem = new TerrainMaterialSystem(
        shaderSystem
    );

    private final Map<Material, List<EntityView>> renderQueue = new Object2ObjectArrayMap<>();

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

        registry.view(Mesh.class, Material.class, Transform.class)
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
        this.renderQueue.computeIfAbsent(entity.get(Material.class), m -> new ArrayList<>()).add(entity);
    }

    private void renderEntitiesFromQueue(Camera camera, PointLight light) {
        for (var batch : renderQueue.entrySet()) {
            Material entityMaterial = batch.getKey();
            List<EntityView> renderables = batch.getValue();
            var shader = entityMaterial.getShader();

            // Bind entityMaterial/shader once
            materialSystem.applyMaterial(entityMaterial);

            // Load global uniforms once per batch
            shaderSystem.setUniform(shader, UNIFORM_PROJECTION_MATRIX, camera.getProjectionMatrix());
            shaderSystem.setUniform(shader, UNIFORM_VIEW_MATRIX, camera.getViewMatrix());
            if (light != null) {
                shaderSystem.setUniform(shader, UNIFORM_LIGHT_POSITION, light.position());
                shaderSystem.setUniform(shader, UNIFORM_LIGHT_COLOR, light.color().toVector3f());
            }

            for (EntityView entity : renderables) {
                shaderSystem.setUniform(shader, UNIFORM_TRANSFORMATION_MATRIX, entity.get(Transform.class).calculateTransformationMatrix());
                meshSystem.render(entity.get(Mesh.class));
            }

            materialSystem.unapplyMaterial(entityMaterial);
        }
    }

    private void clearRenderQueue() {
        this.renderQueue.clear();
    }

}
