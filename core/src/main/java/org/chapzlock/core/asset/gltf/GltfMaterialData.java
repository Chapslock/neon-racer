package org.chapzlock.core.asset.gltf;

import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Engine representation of a glTF PBR material definition.
 *
 * <p><strong>What is PBR (Physically Based Rendering)?</strong> A modern approach to rendering
 * that simulates how light actually behaves in the real world. Instead of tweaking arbitrary
 * numbers (like the old Phong model's "shininess"), PBR uses physical material properties:
 *
 * <ul>
 *   <li><em>Base color</em> — the surface color (like what you'd see if you photographed
 *       the object in perfectly uniform white light)</li>
 *   <li><em>Metallic</em> — is it a metal (1.0) or non-metal (0.0)? Metals reflect light
 *       differently (colored reflections, no diffuse component)</li>
 *   <li><em>Roughness</em> — how smooth is the surface? 0 = perfect mirror, 1 = chalk</li>
 *   <li><em>Normal map</em> — fake bumps and scratches without adding geometry</li>
 *   <li><em>Occlusion</em> — pre-baked shadows in crevices (subtle darkening)</li>
 *   <li><em>Emissive</em> — parts that glow/emit light (neon signs, screens, lava)</li>
 * </ul>
 *
 * <p>This record captures all these properties as parsed from a glTF file, ready to be
 * converted into a {@link org.chapzlock.core.component.PbrMaterial} during scene instantiation.
 *
 * @see GltfAsset
 */
public record GltfMaterialData(
    /** Material name from glTF (for debugging/identification). */
    String name,
    /** Reference to base color texture (file or embedded), or null if none. */
    GltfTextureRef baseColorTexture,
    /** RGBA factor multiplied with base color texture. Default: (1,1,1,1) = white/opaque. */
    Vector4f baseColorFactor,
    /** Reference to metallic-roughness texture, or null. Blue=metallic, Green=roughness. */
    GltfTextureRef metallicRoughnessTexture,
    /** Metallic factor (0-1). Multiplied with texture blue channel if present. */
    float metallicFactor,
    /** Roughness factor (0-1). Multiplied with texture green channel if present. */
    float roughnessFactor,
    /** Reference to tangent-space normal map texture, or null. */
    GltfTextureRef normalTexture,
    /** Reference to ambient occlusion texture (R channel only), or null. */
    GltfTextureRef occlusionTexture,
    /** Reference to emissive texture (RGB), or null. */
    GltfTextureRef emissiveTexture,
    /** RGB emissive factor multiplied with emissive texture. Default: (0,0,0) = no glow. */
    Vector3f emissiveFactor,
    /** Alpha mode: "OPAQUE", "MASK", or "BLEND". */
    String alphaMode,
    /** Alpha cutoff threshold for MASK mode (pixels below this are discarded). Default: 0.5. */
    float alphaCutoff,
    /** If true, both sides of triangles are rendered (disables backface culling). */
    boolean doubleSided
) {
    /** Creates a default opaque white material (used when a mesh has no material assigned). */
    public static GltfMaterialData defaultMaterial() {
        return new GltfMaterialData(
            "default", null, new Vector4f(1, 1, 1, 1), null,
            1.0f, 1.0f, null, null, null,
            new Vector3f(0, 0, 0), "OPAQUE", 0.5f, false
        );
    }
}
