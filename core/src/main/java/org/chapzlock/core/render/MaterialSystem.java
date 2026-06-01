package org.chapzlock.core.render;

import java.util.EnumMap;
import java.util.Map;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.scene.MaterialType;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Associates materials with the renderers that know how to draw them.
 *
 * <p><strong>What this system does:</strong> Each material in the engine has a corresponding
 * {@link MaterialRenderer} implementation that knows how to configure the GPU state (bind
 * shader, set uniforms, bind textures) for that material type. This system maintains the
 * mapping between materials and their renderers.
 *
 * <p><strong>Two registration paths:</strong>
 * <ul>
 *   <li><em>Default renderers</em> — registered once at engine startup for each
 *       {@link MaterialType} (PBR, UI). Used automatically when a material is registered
 *       by type rather than with a specific renderer instance.</li>
 *   <li><em>Custom renderers</em> — registered for specific material instances via
 *       {@link #register(Material, MaterialRenderer)}. Used for custom shader effects that
 *       don't fit the built-in material types.</li>
 * </ul>
 *
 * <p><strong>How registration works:</strong> When a material is registered, its shader is
 * compiled (if not already) and the renderer is stored by shader program ID. During rendering,
 * the render pass looks up the renderer by the material's shader program ID. This means all
 * materials sharing the same shader program use the same renderer instance.
 *
 * <p><strong>Game code usage:</strong> Game code typically doesn't interact with this system
 * directly. The {@link org.chapzlock.core.scene.DescriptorResolver} registers materials
 * during scene instantiation. Custom materials for special effects would be registered via
 * {@link #register(Material, MaterialRenderer)}.
 *
 * @see MaterialRenderer
 * @see MaterialType
 * @see PbrMaterialRenderer
 * @see UiMaterialRenderer
 */
public class MaterialSystem {

    private final ShaderSystem shaderSystem;

    /** Maps shader program ID → renderer. All materials sharing a shader use the same renderer. */
    private final Map<Integer, MaterialRenderer> renderersByShaderId = new Int2ObjectOpenHashMap<>();

    /** Built-in renderers for each material type, registered at engine startup. */
    private final EnumMap<MaterialType, MaterialRenderer> defaultRenderers = new EnumMap<>(MaterialType.class);

    public MaterialSystem(ShaderSystem shaderSystem) {
        this.shaderSystem = shaderSystem;
    }

    /**
     * Registers a material with a specific renderer.
     *
     * <p>Compiles the material's shader if not already compiled, then associates the
     * shader program with the provided renderer. Any material using the same shader
     * program will use this renderer.
     *
     * @param material the material whose shader to register
     * @param renderer the renderer that knows how to draw this material
     */
    public void register(Material material, MaterialRenderer renderer) {
        if (!material.getShader().isCompiled()) {
            shaderSystem.compileAndLink(material.getShader());
        }
        renderersByShaderId.put(material.getShader().getProgramId(), renderer);
    }

    /**
     * Registers a material using the default renderer for a given material type.
     *
     * <p>Convenience method that looks up the pre-registered default renderer for the
     * given type and delegates to {@link #register(Material, MaterialRenderer)}.
     *
     * @param material the material to register
     * @param type the material type (determines which default renderer to use)
     * @throws IllegalStateException if no default renderer is registered for the given type
     */
    public void register(Material material, MaterialType type) {
        MaterialRenderer renderer = defaultRenderers.get(type);
        if (renderer == null) {
            throw new IllegalStateException("No default renderer registered for MaterialType." + type +
                ". Ensure Application has started and registered defaults.");
        }
        register(material, renderer);
    }

    /**
     * Returns the renderer associated with a material's shader program.
     *
     * @param material the material to look up
     * @return the renderer for this material
     * @throws IllegalStateException if no renderer is registered for this material's shader
     */
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

    /**
     * Registers a default renderer for a material type.
     *
     * <p>Called once at engine startup by {@link org.chapzlock.core.application.Application}
     * to set up the built-in renderers (PBR, UI).
     *
     * @param type the material type
     * @param renderer the default renderer for this type
     */
    public void registerDefault(MaterialType type, MaterialRenderer renderer) {
        defaultRenderers.put(type, renderer);
    }

    /**
     * Returns the default renderer for a material type.
     *
     * <p>Used by render passes that need the renderer directly (e.g., OpaquePass uses
     * the PBR default renderer for PbrMaterial entities).
     *
     * @param type the material type
     * @return the default renderer, or null if not registered
     */
    public MaterialRenderer getDefaultRenderer(MaterialType type) {
        return defaultRenderers.get(type);
    }
}
