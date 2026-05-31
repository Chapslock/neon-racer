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
import org.chapzlock.core.ecs.EntityView;
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
 *
 * <p>Both paths batch entities by their respective material component to minimize
 * GPU state changes. The sky color clear and depth buffer clear happen once at the
 * start of {@link #execute}, then legacy batches render first followed by PBR batches.
 *
 * <p><strong>Why two render queues?</strong> The legacy {@link Material} and
 * {@link PbrMaterial} are different component types with incompatible shader interfaces.
 * PBR materials cannot use the {@link MaterialRenderer#apply(RenderContext, Material)}
 * method because that method requires a legacy {@code Material} parameter. Instead,
 * PBR entities use {@link PbrMaterialRenderer#applyPbr(RenderContext, PbrMaterial)}
 * which accepts the PBR-specific component directly.
 *
 * @see PbrMaterialRenderer
 * @see MaterialRenderer
 */
public class OpaquePass implements RenderPass {

    /**
     * Render queue for legacy material entities, batched by {@link Material} instance.
     * Entities sharing the same Material (same shader + textures) are rendered together
     * to avoid redundant GPU state switches.
     */
    private final Map<Material, List<EntityView>> renderQueue = new HashMap<>();

    /**
     * Render queue for PBR material entities, batched by {@link PbrMaterial} instance.
     * Entities sharing the same PbrMaterial are rendered together for the same reason
     * as the legacy queue — minimizing shader and texture binding changes.
     */
    private final Map<PbrMaterial, List<EntityView>> pbrRenderQueue = new HashMap<>();

    @Override
    public int order() {
        return 200;
    }

    @Override
    public void init(EngineContext ctx) {
    }

    /**
     * Collects all opaque entities into material-batched render queues.
     *
     * <p>Two separate ECS queries run each frame:
     * <ol>
     *   <li>Legacy path: entities with {@code OpaqueLayer + Material + Mesh + Transform}</li>
     *   <li>PBR path: entities with {@code OpaqueLayer + PbrMaterial + Mesh + Transform}</li>
     * </ol>
     *
     * <p>Each entity is grouped by its material instance so that {@link #execute} can
     * bind a material once and draw all entities using it before moving to the next.
     *
     * @param ctx the current frame's render context
     */
    @Override
    public void collectVisibles(RenderContext ctx) {
        renderQueue.clear();
        pbrRenderQueue.clear();

        // Legacy material entities
        ctx.getRegistry().view(OpaqueLayer.class, Material.class, Mesh.class, Transform.class)
            .forEach(entity -> renderQueue
                .computeIfAbsent(entity.get(Material.class), m -> new ArrayList<>())
                .add(entity));

        // PBR material entities
        ctx.getRegistry().view(OpaqueLayer.class, PbrMaterial.class, Mesh.class, Transform.class)
            .forEach(entity -> pbrRenderQueue
                .computeIfAbsent(entity.get(PbrMaterial.class), m -> new ArrayList<>())
                .add(entity));
    }

    /**
     * Renders all collected opaque entities.
     *
     * <p>Execution order:
     * <ol>
     *   <li>Clear the color buffer (using sky color if available) and depth buffer</li>
     *   <li>Render all legacy material batches</li>
     *   <li>Render all PBR material batches</li>
     * </ol>
     *
     * <p>The clear happens only once — both material paths render into the same
     * framebuffer with shared depth testing, so PBR and legacy entities correctly
     * occlude each other based on depth.
     *
     * @param ctx the current frame's render context
     */
    @Override
    public void execute(RenderContext ctx) {
        if (ctx.getSky() != null) {
            Color color = ctx.getSky().getColor();
            glClearColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // --- Legacy material rendering ---
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

        // --- PBR material rendering ---
        // PBR uses a dedicated applyPbr() method because PbrMaterial is a different
        // component type than Material and requires PBR-specific uniform setup
        // (metallic, roughness, texture maps, Cook-Torrance BRDF parameters).
        if (!pbrRenderQueue.isEmpty()) {
            PbrMaterialRenderer pbrRenderer = (PbrMaterialRenderer)
                ctx.getMaterialSystem().getDefaultRenderer(MaterialType.PBR);

            for (var batch : pbrRenderQueue.entrySet()) {
                PbrMaterial pbrMaterial = batch.getKey();
                List<EntityView> renderables = batch.getValue();

                pbrRenderer.applyPbr(ctx, pbrMaterial);
                for (EntityView entity : renderables) {
                    pbrRenderer.prepareEntity(ctx, entity);
                    ctx.getMeshSystem().render(entity.get(Mesh.class));
                }
                pbrRenderer.unapply();
            }
        }
    }
}
