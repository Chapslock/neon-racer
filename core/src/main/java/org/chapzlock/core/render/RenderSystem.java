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

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;

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

    @Override
    public void onInit(EngineContext ctx) {
        renderContext = new RenderContext(ctx);
        passes = List.of(new OpaquePass(), new SpritePass());
        passes.forEach(p -> p.init(ctx));

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    @Override
    public SystemPhase phase() {
        return SystemPhase.RENDER;
    }

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
