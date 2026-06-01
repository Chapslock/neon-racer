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
import org.chapzlock.core.component.PbrMaterial;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.scene.MaterialType;

/**
 * Renders all opaque 3D entities (models and terrain), batched by material
 * to minimize shader and texture switches.
 *
 * <p>This pass supports two material systems in parallel:
 * <ul>
 *   <li><strong>Legacy materials</strong> — entities with {@link Material} component,
 *       rendered via the standard {@link MaterialRenderer#apply} path.</li>
 *   <li><strong>PBR materials</strong> — entities with {@link PbrMaterial} component,
 *       rendered via {@link PbrMaterialRenderer#applyPbr} which uses the Cook-Torrance
 *       BRDF shader and PBR-specific uniforms (metallic, roughness, texture maps).</li>
 * </ul>
 */
public class OpaquePass implements RenderPass {

    private final Map<Material, List<Integer>> renderQueue = new HashMap<>();
    private final Map<PbrMaterial, List<Integer>> pbrRenderQueue = new HashMap<>();

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
        pbrRenderQueue.clear();

        ComponentRegistry registry = ctx.getRegistry();

        registry.forEach(OpaqueLayer.class, Material.class, Mesh.class, Transform.class,
            (entityId, layer, material, mesh, transform) -> {
                renderQueue.computeIfAbsent(material, m -> new ArrayList<>()).add(entityId);
            });

        registry.forEach(OpaqueLayer.class, PbrMaterial.class, Mesh.class, Transform.class,
            (entityId, layer, pbrMaterial, mesh, transform) -> {
                pbrRenderQueue.computeIfAbsent(pbrMaterial, m -> new ArrayList<>()).add(entityId);
            });
    }

    @Override
    public void execute(RenderContext ctx) {
        if (ctx.getSky() != null) {
            Color color = ctx.getSky().getColor();
            glClearColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        ComponentRegistry registry = ctx.getRegistry();

        // --- Legacy material rendering ---
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

        // --- PBR material rendering ---
        if (!pbrRenderQueue.isEmpty()) {
            PbrMaterialRenderer pbrRenderer = (PbrMaterialRenderer)
                ctx.getMaterialSystem().getDefaultRenderer(MaterialType.PBR);

            for (var batch : pbrRenderQueue.entrySet()) {
                PbrMaterial pbrMaterial = batch.getKey();
                List<Integer> entityIds = batch.getValue();

                pbrRenderer.applyPbr(ctx, pbrMaterial);
                for (int entityId : entityIds) {
                    pbrRenderer.prepareEntity(ctx, entityId);
                    ctx.getMeshSystem().render(registry.getComponent(entityId, Mesh.class));
                }
                pbrRenderer.unapply();
            }
        }
    }
}
