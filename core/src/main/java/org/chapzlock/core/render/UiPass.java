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
import org.chapzlock.core.ecs.EntityView;

/**
 * Renders all UI sprites in screen-space with orthographic projection.
 *
 * <p>Queries entities with {@link UiLayer}, {@link Material},
 * {@link Mesh}, and {@link Transform} components. GL state management
 * (depth test, blending) is handled by the UiMaterialRenderer.</p>
 */
public class UiPass implements RenderPass {

    private final Map<Material, List<EntityView>> renderQueue = new HashMap<>();

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
        ctx.getRegistry().view(UiLayer.class, Material.class, Mesh.class, Transform.class)
            .forEach(entity -> renderQueue
                .computeIfAbsent(entity.get(Material.class), m -> new ArrayList<>())
                .add(entity));
    }

    @Override
    public void execute(RenderContext ctx) {
        for (var batch : renderQueue.entrySet()) {
            Material material = batch.getKey();
            List<EntityView> renderables = batch.getValue();
            MaterialRenderer renderer = ctx.getMaterialSystem().getRenderer(material);

            renderer.apply(ctx, material);
            for (EntityView entity : renderables) {
                renderer.prepareEntity(ctx, entity);
                ctx.getMeshSystem().render(entity.get(Mesh.class));
            }
            renderer.unapply();
        }
    }
}
