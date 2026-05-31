package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector3f;
import org.joml.Vector4f;

import lombok.Builder;
import lombok.Getter;

/**
 * PBR (Physically Based Rendering) material using the metallic-roughness workflow.
 *
 * <p><strong>What is PBR?</strong> A rendering approach that simulates how light physically
 * interacts with surfaces. Instead of the old Phong model (ambient + diffuse + specular
 * with arbitrary numbers), PBR uses real material properties that produce consistent,
 * realistic results across all lighting conditions. The math models real physics:
 * energy conservation (reflected light never exceeds incoming light), the Fresnel effect
 * (surfaces reflect more at grazing angles), and microfacet theory (roughness is modeled
 * as tiny surface irregularities).
 *
 * <p><strong>Key properties:</strong>
 * <ul>
 *   <li><em>Base color</em> — the inherent color of the surface (like albedo/diffuse in
 *       older models). For metals, this is the specular reflection color. For non-metals,
 *       this is the diffuse reflection color.</li>
 *   <li><em>Metallic</em> — is the surface a metal (1.0) or non-metal/dielectric (0.0)?
 *       Metals have no diffuse reflection and tinted specular. Non-metals have white
 *       specular and colored diffuse. Values between 0 and 1 blend the two.</li>
 *   <li><em>Roughness</em> — how rough is the surface microscopically? 0.0 = perfectly
 *       smooth mirror, 1.0 = completely matte/diffuse. Controls the spread of specular
 *       highlights.</li>
 * </ul>
 *
 * <p><strong>Texture maps:</strong> Each property can optionally be driven by a texture
 * for per-pixel variation. When a texture is present, its sampled value is multiplied by
 * the corresponding scalar/vector factor. When a texture is null, the scalar/vector factor
 * is used uniformly across the entire surface.
 *
 * <p><strong>glTF 2.0 compliance:</strong> This component follows the glTF metallic-roughness
 * material model. Default values match the glTF specification.
 *
 * @see AlphaMode
 * @see Material
 */
@Getter
@Builder
public class PbrMaterial implements Component {

    /**
     * The compiled shader program used to render this material.
     *
     * <p>For PBR materials, this should be a shader implementing the Cook-Torrance BRDF
     * with the metallic-roughness parameterization. The shader must support all the
     * texture maps and factor uniforms defined in this component.
     */
    private final Shader shader;

    /**
     * Base color texture (RGBA).
     *
     * <p>RGB channels contain the surface color (sRGB color space). The alpha channel
     * contains transparency information, used according to the {@link #alphaMode}.
     * If null, {@link #baseColorFactor} is used as a uniform color.
     */
    private final Texture baseColorMap;

    /**
     * Metallic-roughness packed texture.
     *
     * <p>Following the glTF convention: Blue channel = metallic, Green channel = roughness.
     * Red channel is unused (but often stores ambient occlusion in combined textures).
     * Sampled values are multiplied by {@link #metallicFactor} and {@link #roughnessFactor}.
     * If null, the scalar factors are used uniformly.
     */
    private final Texture metallicRoughnessMap;

    /**
     * Tangent-space normal map for surface detail without extra geometry.
     *
     * <p>RGB channels encode the surface normal direction in tangent space, where
     * (0.5, 0.5, 1.0) represents the unperturbed surface normal. This creates the
     * illusion of fine geometric detail (bumps, scratches, pores) without adding
     * triangles to the mesh. If null, the interpolated vertex normal is used.
     */
    private final Texture normalMap;

    /**
     * Ambient occlusion texture (Red channel).
     *
     * <p>Darkens areas where ambient light would be occluded by nearby geometry
     * (crevices, folds, corners). Values range from 0.0 (fully occluded/dark) to
     * 1.0 (fully exposed/bright). Only the red channel is sampled. If null, no
     * ambient occlusion is applied.
     */
    private final Texture occlusionMap;

    /**
     * Emissive texture (RGB, sRGB color space).
     *
     * <p>Makes surfaces appear to emit light/glow. Sampled RGB is multiplied by
     * {@link #emissiveFactor}. Used for glowing panels, neon signs, brake lights,
     * lava, etc. Does not actually cast light onto other objects (that requires a
     * light source entity). If null, {@link #emissiveFactor} alone controls emission.
     */
    private final Texture emissiveMap;

    /**
     * Base color factor multiplied with baseColorMap samples.
     *
     * <p>RGBA, where RGB is the surface color and A is the overall alpha.
     * When {@link #baseColorMap} is present, this factor is multiplied per-component
     * with the texture sample. When absent, this is the uniform surface color.
     * Default: opaque white (1, 1, 1, 1) per glTF spec.
     */
    @Builder.Default
    private final Vector4f baseColorFactor = new Vector4f(1, 1, 1, 1);

    /**
     * Metallic factor in range [0.0, 1.0].
     *
     * <p>Multiplied with the blue channel of {@link #metallicRoughnessMap} if present.
     * 0.0 = dielectric (non-metal, e.g., plastic, wood, skin).
     * 1.0 = metallic (e.g., gold, iron, aluminum).
     * Default: 1.0 per glTF spec (texture controls the actual value).
     */
    @Builder.Default
    private final float metallicFactor = 1.0f;

    /**
     * Roughness factor in range [0.0, 1.0].
     *
     * <p>Multiplied with the green channel of {@link #metallicRoughnessMap} if present.
     * 0.0 = perfectly smooth (mirror-like specular reflection).
     * 1.0 = completely rough (diffuse, no visible specular highlight).
     * Default: 1.0 per glTF spec (texture controls the actual value).
     */
    @Builder.Default
    private final float roughnessFactor = 1.0f;

    /**
     * Emissive factor (RGB linear color space).
     *
     * <p>Multiplied with {@link #emissiveMap} samples if present, or used directly
     * as the emission color. Each channel ranges [0.0, 1.0] (though values above 1.0
     * can be used for HDR bloom effects).
     * Default: black (0, 0, 0) — no emission.
     */
    @Builder.Default
    private final Vector3f emissiveFactor = new Vector3f(0, 0, 0);

    /**
     * How this material handles transparency.
     *
     * <p>Controls whether pixels are rendered as opaque, use alpha testing (MASK),
     * or use alpha blending (BLEND). See {@link AlphaMode} for details on each mode.
     * Default: {@link AlphaMode#OPAQUE}.
     */
    @Builder.Default
    private final AlphaMode alphaMode = AlphaMode.OPAQUE;

    /**
     * Alpha cutoff threshold for {@link AlphaMode#MASK} mode.
     *
     * <p>Pixels with alpha below this value are discarded (not rendered). Pixels at
     * or above this value are rendered as fully opaque. Only used when
     * {@link #alphaMode} is {@link AlphaMode#MASK}. Range: [0.0, 1.0].
     * Default: 0.5 per glTF spec.
     */
    @Builder.Default
    private final float alphaCutoff = 0.5f;

    /**
     * Whether to disable backface culling for this material.
     *
     * <p>If true, both front and back faces of triangles are rendered. Used for
     * thin geometry visible from both sides (leaves, paper, cloth, single-layer
     * walls). If false (default), only front faces are rendered, which is more
     * efficient as it halves the fragment count for solid objects.
     */
    @Builder.Default
    private final boolean doubleSided = false;
}
