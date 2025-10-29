package org.chapzlock.core.system;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.graphics.material.MaterialRenderer;
import org.chapzlock.core.registry.MaterialRendererRegistry;

public class MaterialSystem {
    private static MaterialSystem instance;

    private final MaterialRendererRegistry registry = MaterialRendererRegistry.instance();

    private MaterialSystem() {
    }

    /**
     * Registers a material with a renderer.
     * This renderer defines how the material should be rendered.
     * Every new material declaration needs to be registered with a MaterialRenderer
     * implementation
     *
     * @param material
     * @param renderer
     */
    public void registerNewMaterial(Material material, MaterialRenderer renderer) {
        registry.register(material, renderer);
    }

    /**
     * Fetches the renderer associated with the material
     * @param material
     * @return MaterialRenderer for the material instance
     */
    public MaterialRenderer getRenderer(Material material) {
        return registry.getRenderer(material);
    }

    public static MaterialSystem instance() {
        if (instance == null) {
            instance = new MaterialSystem();
        }
        return instance;
    }
}
