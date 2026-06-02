package org.chapzlock.core.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.SpriteMaterial;
import org.chapzlock.core.component.SpriteLayer;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;

/**
 * Renders all sprite entities in screen-space with orthographic projection.
 *
 * <p>Queries entities with {@link SpriteLayer}, {@link SpriteMaterial},
 * {@link Mesh}, and {@link Transform} components. Batches by material to minimize
 * texture switches, then delegates rendering to {@link SpriteRenderer}.
 *
 * <p><strong>Execution order:</strong> Runs after {@link OpaquePass} (order 800 vs 200)
 * so sprites render on top of the 3D world. Depth testing is disabled during this pass.
 *
 * @see SpriteRenderer
 * @see SpriteMaterial
 */
public class SpritePass implements RenderPass {

    private final Map<SpriteMaterial, List<Integer>> renderQueue = new HashMap<>();
    private SpriteRenderer spriteRenderer;

    @Override
    public int order() {
        return 800;
    }

    @Override
    public void init(EngineContext ctx) {
        spriteRenderer = new SpriteRenderer(
            ctx.getShaderSystem(), ctx.getTextureSystem(), ctx.getWindow().getSpecification());
        spriteRenderer.init();
    }

    @Override
    public void collectVisibles(RenderContext ctx) {
        renderQueue.clear();
        ctx.getRegistry().forEach(SpriteLayer.class, SpriteMaterial.class, Mesh.class, Transform.class,
            (entityId, layer, material, mesh, transform) -> {
                renderQueue.computeIfAbsent(material, m -> new ArrayList<>()).add(entityId);
            });
    }

    @Override
    public void execute(RenderContext ctx) {
        ComponentRegistry registry = ctx.getRegistry();
        for (var batch : renderQueue.entrySet()) {
            SpriteMaterial material = batch.getKey();
            List<Integer> entityIds = batch.getValue();

            spriteRenderer.beginBatch(material);
            for (int entityId : entityIds) {
                Transform transform = registry.getComponent(entityId, Transform.class);
                spriteRenderer.renderEntity(transform);
                ctx.getMeshSystem().render(registry.getComponent(entityId, Mesh.class));
            }
            spriteRenderer.endBatch();
        }
    }
}
