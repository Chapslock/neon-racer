package org.chapzlock.core.render;

import org.chapzlock.core.component.Material;

public class MaterialSystem {

    private final MaterialRendererRegistry registry;

    public MaterialSystem(MaterialRendererRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers a material with a renderer.
     * This renderer defines how the material should be rendered.
     * Every new material declaration needs to be registered with a MaterialRenderer
     * implementation
     *
     * @param material
     * @param renderer
     * @return registered material
     */
    public Material registerNewMaterial(Material material, MaterialRenderer renderer) {
        registry.register(material, renderer);
        return material;
    }

    /**
     * Fetches the renderer associated with the material
     * @param material
     * @return MaterialRenderer for the material instance
     */
    public MaterialRenderer getRenderer(Material material) {
        return registry.getRenderer(material);
    }
}
