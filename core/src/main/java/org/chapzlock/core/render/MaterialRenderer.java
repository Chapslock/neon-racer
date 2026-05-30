package org.chapzlock.core.render;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.ecs.EntityView;

/**
 * Interface for rendering entities with a specific material type.
 *
 * <p>Implementations bind uniforms and textures for their material type.
 * Register via {@link MaterialSystem#register(org.chapzlock.core.component.Material, MaterialRenderer)}.
 */
public interface MaterialRenderer {
    /**
     * Called once per batch before rendering the entities that use the material
     * Used to prepare material for rendering like setting
     * uniforms that are global for the render batch (projection/view, lights, etc)
     */
    void apply(Material material, Camera camera, PointLight light, Sky sky);

    /**
     * Prepare a single entity in the batch for rendering (transforms, per-entity uniforms)
     */
    void prepareEntity(EntityView entity, Material shader);


    /**
     * Called when the batch rendering is finished
     */
    void unapply(Material material);
}
