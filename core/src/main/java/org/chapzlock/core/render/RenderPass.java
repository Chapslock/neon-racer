package org.chapzlock.core.render;

import org.chapzlock.core.application.EngineContext;

/**
 * A single stage in the rendering pipeline.
 *
 * <p>Each pass collects the entities it is responsible for rendering,
 * then executes the draw calls. Passes are registered with RenderSystem
 * and executed in {@link #order()} sequence each frame.</p>
 */
public interface RenderPass {

    /** Execution priority. Lower values run first. */
    int order();

    /** Called once after RenderSystem initialization. */
    void init(EngineContext ctx);

    /** Populate internal render queue from the registry. */
    void collectVisibles(RenderContext ctx);

    /** Issue draw calls for collected entities. */
    void execute(RenderContext ctx);

    /** Called when the window is resized. Override for FBO recreation. */
    default void resize(int width, int height) {}

    /** Release GPU resources owned by this pass. */
    default void dispose() {}
}
