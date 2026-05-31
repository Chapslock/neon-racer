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
        int materialIndex
    ) {}
}
