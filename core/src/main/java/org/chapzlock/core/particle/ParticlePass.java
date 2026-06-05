package org.chapzlock.core.particle;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.render.RenderContext;
import org.chapzlock.core.render.RenderPass;
import org.chapzlock.core.render.TextureRegistry;

/**
 * Renders all particle emitters with instanced billboarded quads.
 *
 * <p>Queries all entities with {@link ParticleEmitter} components, groups them by texture,
 * and issues instanced draw calls via {@link ParticleRenderer}. Runs at order 600 (after
 * opaque geometry, before sprites) with additive blending and depth writes disabled.
 *
 * <p><strong>Why disable depth writes?</strong> Particles are transparent and rendered back-to-front.
 * If they wrote to the depth buffer, they would occlude other particles behind them even though
 * they're semi-transparent. Disabling depth writes lets particles blend correctly with each other.
 *
 * @see ParticleRenderer
 * @see ParticleEmitter
 */
public class ParticlePass implements RenderPass {

    private static final int INITIAL_INSTANCE_CAPACITY = 1024;

    private final Map<Texture, List<ParticleEmitter>> renderQueue = new HashMap<>();
    private ParticleRenderer renderer;
    private TextureRegistry textureRegistry;

    @Override
    public int order() {
        return 600;
    }

    @Override
    public void init(Engine engine) {
        this.textureRegistry = engine.getTextureRegistry();
        this.renderer = new ParticleRenderer(engine.getShaderRegistry(), textureRegistry);
        renderer.init(INITIAL_INSTANCE_CAPACITY);
    }

    @Override
    public void collectVisibles(RenderContext ctx) {
        renderQueue.clear();

        ComponentRegistry registry = ctx.getRegistry();

        registry.forEach(ParticleEmitter.class, (entityId, emitter) -> {
            if (emitter.getAliveCount() > 0) {
                Texture texture = resolveTexture(emitter);
                renderQueue.computeIfAbsent(texture, t -> new ArrayList<>()).add(emitter);
            }
        });
    }

    @Override
    public void execute(RenderContext ctx) {
        if (renderQueue.isEmpty()) {
            return;
        }

        // Set GL state for particle rendering
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);  // disable depth writes
        glDisable(GL_CULL_FACE);

        // Render each batch (grouped by texture)
        for (var batch : renderQueue.entrySet()) {
            Texture texture = batch.getKey();
            List<ParticleEmitter> emitters = batch.getValue();

            for (ParticleEmitter emitter : emitters) {
                renderer.render(ctx, emitter.getParticles(), emitter.getAliveCount(), texture);
            }
        }

        // Restore GL state
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
    }

    @Override
    public void dispose() {
        if (renderer != null) {
            renderer.dispose();
            renderer = null;
        }
    }

    /**
     * Resolves the texture for a given emitter. Falls back to default texture if none specified.
     */
    private Texture resolveTexture(ParticleEmitter emitter) {
        String texturePath = emitter.getEffect().getTexturePath();
        if (texturePath != null && !texturePath.isEmpty()) {
            return textureRegistry.load(texturePath);
        }
        return renderer.getDefaultTexture();
    }
}
