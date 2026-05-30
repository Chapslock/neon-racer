package org.chapzlock.core.component;

import java.util.List;

import org.chapzlock.core.ecs.Component;

import lombok.Builder;
import lombok.Getter;

/**
 * Describes procedurally generated terrain from a heightmap image.
 *
 * <p>At scene load time, the engine generates a mesh from the heightmap,
 * loads splat textures with a blend map, and creates the terrain material.
 * The descriptor is consumed during instantiation.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * TerrainDescriptor.builder()
 *     .heightmap("textures/terrain/heightmap.png")
 *     .maxHeight(40f)
 *     .textures(List.of("grass.png", "dirt.png", "concrete.png"))
 *     .blendMap("textures/terrain/blendMap.png")
 *     .build()
 * }</pre>
 */
@Getter
@Builder
public class TerrainDescriptor implements Component {

    /** Path to the heightmap image. Pixel brightness determines vertex height. */
    private final String heightmap;

    /** Maximum terrain elevation in world units. */
    private final float maxHeight;

    /** Terrain size in world units (width and depth). */
    @Builder.Default
    private final float size = 800f;

    /** Number of vertices along each axis of the terrain grid. */
    @Builder.Default
    private final int vertexCount = 128;

    /** Paths to splat textures blended across the terrain surface. */
    @Builder.Default
    private final List<String> textures = List.of();

    /** Path to the RGB blend map controlling texture mixing. */
    private final String blendMap;
}
