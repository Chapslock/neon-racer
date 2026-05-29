package org.chapzlock.core.render;

import java.util.EnumMap;
import java.util.Map;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.scene.MaterialType;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Links materials to material renderers
 */
public class MaterialRendererRegistry {

    private final ShaderSystem shaderSystem;
    private final Map<Integer, MaterialRenderer> renderersByShaderId = new Int2ObjectOpenHashMap<>();
    private final EnumMap<MaterialType, MaterialRenderer> defaultRenderers = new EnumMap<>(MaterialType.class);

    public MaterialRendererRegistry(ShaderSystem shaderSystem) {
        this.shaderSystem = shaderSystem;
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

    /**
     * Registers a default renderer for the given material type.
     *
     * @param type the material type
     * @param renderer the default renderer for this material type
     */
    public void registerDefault(MaterialType type, MaterialRenderer renderer) {
        defaultRenderers.put(type, renderer);
    }

    /**
     * Retrieves the default renderer for the given material type.
     *
     * @param type the material type
     * @return the default renderer, or null if none is registered
     */
    public MaterialRenderer getDefaultRenderer(MaterialType type) {
        return defaultRenderers.get(type);
    }
}
