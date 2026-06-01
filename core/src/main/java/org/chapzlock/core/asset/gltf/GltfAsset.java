package org.chapzlock.core.asset.gltf;

import java.util.List;

/**
 * Top-level container for all data parsed from a glTF/GLB file.
 *
 * <p>This is the CPU-side result of loading a 3D asset file. It contains everything
 * needed to instantiate a scene: the node tree (hierarchy), meshes (geometry),
 * materials (PBR properties), skins (skeleton rigs), and animations (keyframed clips).
 *
 * <p><strong>Lifecycle:</strong>
 * <ol>
 *   <li>{@link GltfLoader} parses the file and creates this object</li>
 *   <li>Scene instantiation code reads this to create entities with components</li>
 *   <li>This object is then discarded (the engine keeps GPU resources, not this data)</li>
 * </ol>
 *
 * <p><strong>Cross-referencing:</strong> Data in this asset references other data by index.
 * A node's {@code meshIndex} points into {@link #meshes()}, a primitive's
 * {@code materialIndex} points into {@link #materials()}, a node's {@code childIndices}
 * point into {@link #nodes()}, a node's {@code skinIndex} points into {@link #skins()},
 * and animation channel targets reference {@link #nodes()} by index.
 *
 * @see GltfLoader
 */
public record GltfAsset(
    /** All nodes in the file (flat list; tree structure via node.childIndices). */
    List<GltfNodeData> nodes,
    /** Indices into nodes() for root-level nodes (those with no parent) in the default scene. */
    List<Integer> rootNodeIndices,
    /** All meshes in the file. Referenced by nodes via meshIndex. */
    List<GltfMeshData> meshes,
    /** All materials in the file. Referenced by mesh primitives via materialIndex. */
    List<GltfMaterialData> materials,
    /** All skins (skeleton rigs) in the file. Referenced by nodes via skinIndex. */
    List<GltfSkinData> skins,
    /** All animation clips in the file. Each targets nodes by index into {@link #nodes()}. */
    List<GltfAnimationData> animations
) {
    /** Returns the total number of nodes in this asset. */
    public int nodeCount() { return nodes.size(); }
}
