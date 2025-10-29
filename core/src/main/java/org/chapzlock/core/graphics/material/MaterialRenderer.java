package org.chapzlock.core.graphics.material;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.entity.EntityView;

/**
 * After creating a new material you need to provide instructions for the render system to render it
 * An implementation of this class is required to render a material correctly.
 */
public interface MaterialRenderer {
    /**
     * Called once per batch before rendering the entities that use the material
     * Used to prepare material for rendering like setting
     * uniforms that are global for the render batch (projection/view, lights, etc)
     */
    void apply(Material material, Camera camera, PointLight light);

    /**
     * Prepare a single entity in the batch for rendering (transforms, per-entity uniforms)
     */
    void prepareEntity(EntityView entity, Material shader);


    /**
     * Called when the batch rendering is finished
     */
    void unapply(Material material);
}
