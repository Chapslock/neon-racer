package org.chapzlock.core.render;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.ecs.EntityView;

/**
 * Rendering strategy for a specific material type.
 *
 * <p>Each implementation knows how to bind the correct uniforms and textures
 * for its material type. The {@link RenderContext} provides per-frame state
 * (camera matrices, lighting) so renderers don't query the registry themselves.</p>
 */
public interface MaterialRenderer {

    /** Bind shader, set per-batch uniforms (projection, view, lighting). */
    void apply(RenderContext ctx, Material material);

    /** Set per-entity uniforms (transformation matrix). */
    void prepareEntity(RenderContext ctx, EntityView entity);

    /** Unbind shader, restore GL state modified during apply(). */
    void unapply();
}
