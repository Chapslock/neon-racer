package org.chapzlock.core.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.component.UiLayer;
import org.chapzlock.core.ecs.ComponentRegistry;

/**
 * Renders all UI sprites in screen-space with orthographic projection.
 *
 * <p>Queries entities with {@link UiLayer}, {@link Material},
 * {@link Mesh}, and {@link Transform} components. GL state management
 * (depth test, blending) is handled by the UiMaterialRenderer.</p>
 */
public class UiPass implements RenderPass {

    private final Map<Material, List<Integer>> renderQueue = new HashMap<>();

    @Override
    public int order() {
        return 800;
    }

    @Override
    public void init(EngineContext ctx) {
    }

    @Override
    public void collectVisibles(RenderContext ctx) {
        renderQueue.clear();
        ctx.getRegistry().forEach(UiLayer.class, Material.class, Mesh.class, Transform.class,
            (entityId, layer, material, mesh, transform) -> {
                renderQueue.computeIfAbsent(material, m -> new ArrayList<>()).add(entityId);
            });
    }

    @Override
    public void execute(RenderContext ctx) {
        ComponentRegistry registry = ctx.getRegistry();
        for (var batch : renderQueue.entrySet()) {
            Material material = batch.getKey();
            List<Integer> entityIds = batch.getValue();
            MaterialRenderer renderer = ctx.getMaterialSystem().getRenderer(material);

            renderer.apply(ctx, material);
            for (int entityId : entityIds) {
                renderer.prepareEntity(ctx, entityId);
                ctx.getMeshSystem().render(registry.getComponent(entityId, Mesh.class));
            }
            renderer.unapply();
        }
    }
}
