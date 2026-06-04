package org.chapzlock.core.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.OpaqueLayer;
import org.chapzlock.core.component.PbrMaterial;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;

/**
 * Renders all opaque 3D entities using PBR (physically-based rendering) materials.
 *
 * <p>Entities are batched by {@link PbrMaterial} to minimize shader and texture switches.
 * Each batch binds the PBR shader once, sets material-level uniforms, then draws all
 * entities sharing that material before moving to the next batch.
 *
 * <p><strong>Requirements for an entity to be rendered by this pass:</strong>
 * <ul>
 *   <li>{@link OpaqueLayer} tag — marks the entity as a 3D opaque object</li>
 *   <li>{@link PbrMaterial} — material properties (textures, metallic, roughness)</li>
 *   <li>{@link Mesh} — the 3D geometry to draw</li>
 *   <li>{@link Transform} — world position/rotation/scale</li>
 * </ul>
 *
 * @see PbrMaterialRenderer
 * @see PbrMaterial
 */
public class OpaquePass implements RenderPass {

    private final Map<PbrMaterial, List<Integer>> pbrRenderQueue = new HashMap<>();
    private PbrMaterialRenderer pbrRenderer;

    @Override
    public int order() {
        return 200;
    }

    @Override
    public void init(EngineContext ctx) {
        pbrRenderer = new PbrMaterialRenderer(ctx.getShaderSystem(), ctx.getTextureSystem());
    }

    @Override
    public void collectVisibles(RenderContext ctx) {
        pbrRenderQueue.clear();

        ComponentRegistry registry = ctx.getRegistry();

        registry.forEach(OpaqueLayer.class, PbrMaterial.class, Mesh.class, Transform.class,
            (entityId, layer, pbrMaterial, mesh, transform) -> {
                pbrRenderQueue.computeIfAbsent(pbrMaterial, m -> new ArrayList<>()).add(entityId);
            });
    }

    @Override
    public void execute(RenderContext ctx) {
        ComponentRegistry registry = ctx.getRegistry();

        if (!pbrRenderQueue.isEmpty()) {
            for (var batch : pbrRenderQueue.entrySet()) {
                PbrMaterial pbrMaterial = batch.getKey();
                List<Integer> entityIds = batch.getValue();

                pbrRenderer.applyPbr(ctx, pbrMaterial);
                for (int entityId : entityIds) {
                    pbrRenderer.prepareEntity(ctx, entityId);
                    ctx.getMeshRegistry().render(registry.getComponent(entityId, Mesh.class));
                }
                pbrRenderer.unapply();
            }
        }
    }
}
