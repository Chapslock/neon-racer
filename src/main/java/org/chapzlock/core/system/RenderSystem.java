package org.chapzlock.core.system;

import org.chapzlock.core.component.CameraComponent;
import org.chapzlock.core.component.orchestration.ComponentRegistry;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.component.MaterialComponent;
import org.chapzlock.core.component.MeshComponent;
import org.chapzlock.core.component.TransformComponent;
import org.chapzlock.core.math.MathUtil;
import org.chapzlock.core.math.Matrix4f;

public class RenderSystem implements System {
    private final ComponentRegistry registry;

    public RenderSystem(ComponentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onRender(float deltaTime) {
        for (EntityView e : registry.view(MeshComponent.class, MaterialComponent.class, TransformComponent.class, CameraComponent.class)) {
            TransformComponent transform = e.get(TransformComponent.class);
            MeshComponent meshComponent = e.get(MeshComponent.class);
            MaterialComponent materialComponent = e.get(MaterialComponent.class);
            CameraComponent camera = e.get(CameraComponent.class);

            materialComponent.material().bind();

            materialComponent.material().getShader().loadTransformationMatrix(MathUtil.createTransformationMatrix(
                transform.getPosition(),
                transform.getRotation(),
                transform.getScale()
            ));
            materialComponent.material().getShader().loadProjectionMatrix(Matrix4f.perspective(
                70f,
                600f / 400f,
                0.1f,
                1000f
            ));

            materialComponent.material().getShader().loadViewMatrix(MathUtil.createViewMatrix(
                camera.getPosition(),
                camera.getRotation()
            ));

            meshComponent.mesh().render();

            materialComponent.material().unbind();

        }
    }
}
