package org.chapzlock.core.render;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glEnable;

import java.util.List;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.particle.ParticlePass;
import org.chapzlock.core.window.WindowResizeEvent;

/**
 * Orchestrates the rendering pipeline by executing registered RenderPasses in order.
 *
 * <p>Each frame: populates RenderContext with per-frame state (camera, lights),
 * then iterates passes in order — each pass collects its visible entities and
 * issues draw calls independently.</p>
 */
public class RenderSystem implements GameSystem {

    private List<RenderPass> passes;
    private RenderContext renderContext;

    /** {@inheritDoc} Initializes render passes (OpaquePass, ParticlePass, SpritePass, TextPass) and the shared RenderContext. */
    @Override
    public void onInit(Engine engine) {
        renderContext = new RenderContext(engine);
        passes = List.of(new OpaquePass(), new ParticlePass(), new SpritePass(), new TextPass());
        passes.forEach(p -> p.init(engine));

        engine.getEventBus().subscribe(WindowResizeEvent.class, event -> {
            for (RenderPass pass : passes) {
                pass.resize((int) event.width(), (int) event.height());
            }
            return false;
        });

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    /** Returns {@link SystemPhase#RENDER} — this system executes after all logic and physics. */
    @Override
    public SystemPhase phase() {
        return SystemPhase.RENDER;
    }

    /**
     * Executes the full rendering pipeline for one frame.
     *
     * <p>Steps:
     * <ol>
     *   <li>Clear the framebuffer (color + depth)</li>
     *   <li>Populate RenderContext with camera matrices, lights, and fog from the current frame</li>
     *   <li>Execute each RenderPass in order — each pass collects its visible entities and issues draw calls</li>
     * </ol>
     */
    @Override
    public void onRender(float deltaTime) {
        if (!renderContext.beginFrame()) {
            return;
        }
        var sky = renderContext.getSky();
        if (sky != null) {
            var color = sky.getColor();
            glClearColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        } else {
            glClearColor(0f, 0f, 0f, 1f);
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        for (RenderPass pass : passes) {
            pass.collectVisibles(renderContext);
            pass.execute(renderContext);
        }
    }
}
