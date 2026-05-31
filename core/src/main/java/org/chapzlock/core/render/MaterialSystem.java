package org.chapzlock.core.render;

import java.util.EnumMap;
import java.util.Map;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.scene.MaterialType;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Manages material-to-renderer associations.
 *
 * <p>Each {@link Material} is paired with a {@link MaterialRenderer} that knows how to render it.
 * Default renderers are registered for built-in material types (ENTITY, PBR, UI).
 */
public class MaterialSystem {

    private final ShaderSystem shaderSystem;
    private final Map<Integer, MaterialRenderer> renderersByShaderId = new Int2ObjectOpenHashMap<>();
    private final EnumMap<MaterialType, MaterialRenderer> defaultRenderers = new EnumMap<>(MaterialType.class);

    public MaterialSystem(ShaderSystem shaderSystem) {
        this.shaderSystem = shaderSystem;
    }

    public void register(Material material, MaterialRenderer renderer) {
        if (!material.getShader().isCompiled()) {
            shaderSystem.compileAndLink(material.getShader());
        }
        renderersByShaderId.put(material.getShader().getProgramId(), renderer);
    }

    public void register(Material material, MaterialType type) {
        MaterialRenderer renderer = defaultRenderers.get(type);
        if (renderer == null) {
            throw new IllegalStateException("No default renderer registered for MaterialType." + type +
                ". Ensure Application has started and registered defaults.");
        }
        register(material, renderer);
    }

    public MaterialRenderer getRenderer(Material material) {
        var renderer = renderersByShaderId.get(material.getShader().getProgramId());
        if (renderer == null) {
            throw new IllegalStateException(
                "No renderer registered for shaders: " + material.getShader().getVertexPath() +
                    " and " + material.getShader().getFragmentPath() +
                    ". Call materialSystem.register(material, type) before rendering.");
        }
        return renderer;
    }

    public void registerDefault(MaterialType type, MaterialRenderer renderer) {
        defaultRenderers.put(type, renderer);
    }

    public MaterialRenderer getDefaultRenderer(MaterialType type) {
        return defaultRenderers.get(type);
    }
}
