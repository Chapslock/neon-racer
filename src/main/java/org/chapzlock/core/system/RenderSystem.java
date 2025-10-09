package org.chapzlock.core.system;

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
import org.chapzlock.core.math.Matrix4f;

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
        CameraComponent camera = registry.view(CameraComponent.class).getFirst().get(CameraComponent.class);
        if (camera == null) {
            Log.error("No Camera found! Skipping rendering");
            return;
        }

        Matrix4f projectionMatrix = Matrix4f.perspective(
                FIELD_OF_VIEW,
                Application.get().getAppSpec().getWindowSpec().getAspectRatio(),
                NEAR_PLANE,
                FAR_PLANE
        );

        Matrix4f viewMatrix = MathUtil.createViewMatrix(
                camera.getPosition(),
                camera.getRotation()
        );

        LightComponent light = registry.view(LightComponent.class).getFirst().get(LightComponent.class);

        // --- 3. Render all entities with Mesh + Material + Transform
        for (EntityView entity : registry.view(MeshComponent.class, MaterialComponent.class, TransformComponent.class)) {
            MeshComponent mesh = entity.get(MeshComponent.class);
            MaterialComponent material = entity.get(MaterialComponent.class);
            TransformComponent transform = entity.get(TransformComponent.class);

            material.material().bind();

            material.material().getShader().loadTransformationMatrix(
                MathUtil.createTransformationMatrix(
                    transform.getPosition(),
                    transform.getRotation(),
                    transform.getScale()
                )
            );
            material.material().getShader().loadProjectionMatrix(projectionMatrix);
            material.material().getShader().loadViewMatrix(viewMatrix);

            if (light != null) {
                material.material().getShader().loadLight(light);
            }

            mesh.mesh().render();

            material.material().unbind();
        }
    }

}
