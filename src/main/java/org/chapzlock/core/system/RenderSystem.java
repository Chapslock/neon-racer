package org.chapzlock.core.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.Application;
import org.chapzlock.core.component.CameraComponent;
import org.chapzlock.core.component.LightComponent;
import org.chapzlock.core.component.MaterialComponent;
import org.chapzlock.core.component.MeshComponent;
import org.chapzlock.core.component.TransformComponent;
import org.chapzlock.core.component.orchestration.ComponentRegistry;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.math.MathUtil;
import org.joml.Matrix4f;

public class RenderSystem implements System {
    private static final float FIELD_OF_VIEW = 70f;
    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 1000f;
    private final ComponentRegistry registry;

    public RenderSystem(ComponentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onRender(float deltaTime) {
        CameraComponent camera = registry.view(CameraComponent.class)
            .getFirst()
            .get(CameraComponent.class);

        if (camera == null) {
            Log.error("No Camera found! Skipping rendering");
            return;
        }

        // --- Projection & View (calculated once per frame)
        Matrix4f projectionMatrix = new Matrix4f().perspective(
            FIELD_OF_VIEW,
            Application.get().getAppSpec().getWindowSpec().getAspectRatio(),
            NEAR_PLANE,
            FAR_PLANE
        );

        Matrix4f viewMatrix = MathUtil.createViewMatrix(
            camera.getPosition(),
            camera.getRotation()
        );

        LightComponent light = registry.view(LightComponent.class)
            .getFirst()
            .get(LightComponent.class);

        // --- Group entities by material (or at least by shader)
        Map<MaterialComponent, List<EntityView>> materialBatches = new HashMap<>();
        for (EntityView entity : registry.view(MeshComponent.class, MaterialComponent.class, TransformComponent.class)) {
            MaterialComponent material = entity.get(MaterialComponent.class);
            materialBatches.computeIfAbsent(material, m -> new ArrayList<>()).add(entity);
        }

        // --- Render batches
        for (Map.Entry<MaterialComponent, List<EntityView>> entry : materialBatches.entrySet()) {
            MaterialComponent material = entry.getKey();
            var shader = material.material().getShader();

            // Bind material & shader once
            material.material().bind();

            // Load global (per-frame) uniforms once per shader
            shader.loadProjectionMatrix(projectionMatrix);
            shader.loadViewMatrix(viewMatrix);
            if (light != null) {
                shader.loadLight(light);
            }

            // Draw each entity in this batch
            for (EntityView entity : entry.getValue()) {
                MeshComponent mesh = entity.get(MeshComponent.class);
                TransformComponent transform = entity.get(TransformComponent.class);

                // Per-entity model matrix
                shader.loadTransformationMatrix(
                    MathUtil.createTransformationMatrix(
                        transform.getPosition(),
                        transform.getRotation(),
                        transform.getScale()
                    )
                );

                mesh.mesh().render();
            }

            // Unbind once per batch
            material.material().unbind();
        }
    }

}
