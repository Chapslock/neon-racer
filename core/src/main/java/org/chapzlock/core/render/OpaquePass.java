package org.chapzlock.core.render;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.OpaqueLayer;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.EntityView;

/**
 * Renders all opaque 3D entities (models and terrain), batched by material
 * to minimize shader and texture switches.
 *
 * <p>Queries entities with {@link OpaqueLayer}, {@link Material},
 * {@link Mesh}, and {@link Transform} components.</p>
 */
public class OpaquePass implements RenderPass {

    private final Map<Material, List<EntityView>> renderQueue = new HashMap<>();

    @Override
    public int order() {
        return 200;
    }

    @Override
    public void init(EngineContext ctx) {
    }

    @Override
    public void collectVisibles(RenderContext ctx) {
        renderQueue.clear();
        ctx.getRegistry().view(OpaqueLayer.class, Material.class, Mesh.class, Transform.class)
            .forEach(entity -> renderQueue
                .computeIfAbsent(entity.get(Material.class), m -> new ArrayList<>())
                .add(entity));
    }

    @Override
    public void execute(RenderContext ctx) {
        if (ctx.getSky() != null) {
            Color color = ctx.getSky().getColor();
            glClearColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

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
