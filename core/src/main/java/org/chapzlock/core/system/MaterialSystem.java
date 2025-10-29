package org.chapzlock.core.system;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.graphics.material.MaterialRenderer;
import org.chapzlock.core.registry.MaterialRendererRegistry;

public class MaterialSystem {
    private static MaterialSystem instance;

    private final MaterialRendererRegistry registry = MaterialRendererRegistry.instance();

    private MaterialSystem() {
    }

    public void registerNewMaterial(Material material, MaterialRenderer renderer) {
        registry.register(material, renderer);
    }

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
