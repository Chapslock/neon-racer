package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.Builder;
import lombok.Getter;

/**
 * Describes a 3D model to be loaded and rendered.
 *
 * <p>At scene load time, the engine resolves this into a {@link Mesh},
 * {@link Material}, and {@link Shader} with the appropriate renderer.
 * The descriptor is consumed during instantiation and not retained
 * on the entity.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ModelDescriptor.of("obj/car.obj", "textures/car.png")
 *
 * ModelDescriptor.builder()
 *     .mesh("obj/car.obj")
 *     .texture("textures/car.png")
 *     .reflectivity(0.5f)
 *     .shineDamper(32f)
 *     .build()
 * }</pre>
 */
@Getter
@Builder
public class ModelDescriptor implements Component {

    /** Path to the .obj mesh file relative to resources. */
    private final String mesh;

    /** Path to the diffuse texture file. Null for untextured (tint-only) models. */
    private final String texture;

    /** Specular intensity. Higher values produce brighter highlights. */
    @Builder.Default
    private final float reflectivity = 0.1f;

    /** Specular exponent. Higher values produce tighter, sharper highlights. */
    @Builder.Default
    private final float shineDamper = 1f;

    /** Path to normal map texture. Null if not normal-mapped. Reserved for Phase 8. */
    private final String normalMap;

    /** Whether this model is rendered in the transparency pass with alpha blending. */
    @Builder.Default
    private final boolean transparent = false;

    /** Whether this model casts shadows onto other surfaces. Reserved for Phase 3. */
    @Builder.Default
    private final boolean castShadows = false;

    /** Whether this model receives shadows from other objects. Reserved for Phase 3. */
    @Builder.Default
    private final boolean receiveShadows = true;

    /** Convenience factory for the common case: mesh + texture with defaults. */
    public static ModelDescriptor of(String mesh, String texture) {
        return builder().mesh(mesh).texture(texture).build();
    }

    /** Convenience factory for untextured model (rendered with tint color). */
    public static ModelDescriptor of(String mesh) {
        return builder().mesh(mesh).build();
    }
}
