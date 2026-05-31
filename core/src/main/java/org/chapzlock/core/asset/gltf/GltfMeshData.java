package org.chapzlock.core.asset.gltf;

import java.util.List;

import org.chapzlock.core.asset.RawMeshData;

/**
 * Engine representation of a single glTF mesh.
 *
 * <p>A glTF mesh can contain multiple "primitives" (sub-meshes), each with its own
 * material. Think of a character model: the body might be one primitive with a skin
 * material, and the armor another primitive with a metal material. In the engine,
 * each primitive typically becomes a separate renderable entity.
 *
 * @see GltfNodeData
 * @see GltfAsset
 */
public record GltfMeshData(
    /** Name of the mesh from the glTF file (may be null if unnamed). */
    String name,
    /** One entry per primitive (sub-mesh). Each has its own geometry and material reference. */
    List<Primitive> primitives
) {
    /**
     * A single primitive (sub-mesh) within a glTF mesh.
     *
     * <p>Each primitive has its own vertex data and references a material by index.
     * Different primitives can use different materials, enabling multi-material meshes
     * (e.g., a car with paint on the body and chrome on the bumper).
     */
    public record Primitive(
        /** The vertex data (positions, normals, UVs, etc.) for this sub-mesh. */
        RawMeshData meshData,
        /** Index into {@link GltfAsset#materials()}, or -1 if no material assigned. */
        int materialIndex,
        /**
         * Morph targets (blend shapes) for this primitive.
         *
         * <p>Each entry represents one morph target containing per-vertex position and/or
         * normal deltas. During rendering, these deltas are blended using weights and added
         * to the base vertex data. An empty list means no morph targets are present.
         *
         * <p>Morph targets are commonly used for facial expressions (smile, frown, blink),
         * corrective shapes (fixing deformation artifacts from skeletal animation), and
         * any deformation that is difficult to achieve with bones alone.
         */
        List<MorphTargetData> morphTargets
    ) {}

    /**
     * Position and normal deltas for a single morph target (blend shape).
     *
     * <p>A morph target stores per-vertex offsets that are added to the base mesh when
     * the target is active. The blending formula for each vertex is:
     * <pre>{@code
     * finalPosition = basePosition + weight * positionDelta
     * finalNormal   = normalize(baseNormal + weight * normalDelta)
     * }</pre>
     *
     * <p>When multiple targets are active simultaneously, their weighted deltas are summed:
     * <pre>{@code
     * finalPosition = basePosition + sum(weight[i] * positionDelta[i])
     * }</pre>
     *
     * <p>Weights range from 0.0 (target inactive) to 1.0 (fully applied), though values
     * outside this range are permitted for exaggerated effects.
     *
     * @param positionDeltas position offsets as a flat float array (vec3 per vertex:
     *                       [dx0, dy0, dz0, dx1, dy1, dz1, ...]). May be null if this
     *                       target only affects normals (rare but valid in glTF).
     * @param normalDeltas   normal offsets as a flat float array (vec3 per vertex), or null
     *                       if the target does not provide normal deltas. When null, normals
     *                       can be recomputed from the morphed positions if needed.
     */
    public record MorphTargetData(
        float[] positionDeltas,
        float[] normalDeltas
    ) {}
}
