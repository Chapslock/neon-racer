package org.chapzlock.core.registry;

import java.util.Map;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.graphics.material.MaterialRenderer;
import org.chapzlock.core.system.ShaderSystem;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Links materials to material renderers
 */
public class MaterialRendererRegistry {
    private static final ShaderSystem shaderSystem = ShaderSystem.instance();
    private static MaterialRendererRegistry instance;
    private final Map<Integer, MaterialRenderer> renderersByShaderId = new Int2ObjectOpenHashMap<>();

    private MaterialRendererRegistry() {
    }

    /**
     * Registers a new association for a material and its renderer.
     * This association will be used by render system to render materials correctly.
     *
     * @param material a new type of material
     * @param renderer implementation of MaterialRenderer that knows how to render this material
     */
    public void register(Material material, MaterialRenderer renderer) {
        if (!material.getShader().isCompiled()) {
            shaderSystem.compileAndLink(material.getShader());
        }
        renderersByShaderId.put(material.getShader().getProgramId(), renderer);
    }

    /**
     * Fetches the correct renderer for the given material component
     *
     * @param material
     * @return materialRenderer associated with the material
     */
    public MaterialRenderer getRenderer(Material material) {
        var renderer = renderersByShaderId.get(material.getShader().getProgramId());
        if (renderer == null) {
            String vertexShaderName = material.getShader().getVertexPath();
            String fragmentShaderName = material.getShader().getFragmentPath();
            throw new IllegalStateException(
                "Could not find instructions to render shaders: " + vertexShaderName + " and " + fragmentShaderName +
                    "\n Make sure you have implemented and registered renderer for this material");
        }
        return renderer;
    }

    public static MaterialRendererRegistry instance() {
        if (instance == null) {
            instance = new MaterialRendererRegistry();
        }
        return instance;
    }
}
