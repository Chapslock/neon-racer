package org.chapzlock.core.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.Application;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.graphics.Camera;
import org.chapzlock.core.graphics.Material;
import org.chapzlock.core.graphics.Mesh;
import org.chapzlock.core.graphics.PointLight;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.registry.ComponentRegistry;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import lombok.RequiredArgsConstructor;

/**
 * The core render system
 */
@RequiredArgsConstructor
public class RenderSystem implements System {
    private static final float FIELD_OF_VIEW = 70f;
    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 1000f;
    private final ComponentRegistry registry;

    private Matrix4f projectionMatrix = new Matrix4f();

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

        // --- Projection & View (calculated once per frame)
        this.projectionMatrix.identity();
        Matrix4f projectionMatrix = this.projectionMatrix.perspective(
            FIELD_OF_VIEW,
            Application.get().getAppSpec().getWindowSpec().getAspectRatio(),
            NEAR_PLANE,
            FAR_PLANE
        );

        Matrix4f viewMatrix = camera.getViewMatrix();

        PointLight light = registry.view(PointLight.class)
            .getFirst()
            .get(PointLight.class);

        // --- Group entities by material (or at least by shader)
        Map<Material, List<EntityView>> materialBatches = new HashMap<>();
        for (EntityView entity : registry.view(Mesh.class, Material.class, Transform.class)) {
            Material material = entity.get(Material.class);
            materialBatches.computeIfAbsent(material, m -> new ArrayList<>()).add(entity);
        }

        // --- Render batches
        for (Map.Entry<Material, List<EntityView>> entry : materialBatches.entrySet()) {
            Material material = entry.getKey();
            var shader = material.getShader();

            // Bind material & shader once
            material.bind();

            // Load global (per-frame) uniforms once per shader
            shader.loadProjectionMatrix(projectionMatrix);
            shader.loadViewMatrix(viewMatrix);
            if (light != null) {
                shader.loadLight(light);
            }
            shader.loadShine(material.getReflection().getShineDamper(), material.getReflection().getReflectivity());

            // Draw each entity in this batch
            for (EntityView entity : entry.getValue()) {
                Mesh mesh = entity.get(Mesh.class);
                Transform transform = entity.get(Transform.class);

                // Per-entity model matrix
                shader.loadTransformationMatrix(
                    transform.calculateTransformationMatrix()
                );

                mesh.render();
            }

            // Unbind once per batch
            material.unbind();
        }
    }

}
