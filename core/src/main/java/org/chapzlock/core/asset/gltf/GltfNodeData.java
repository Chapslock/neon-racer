package org.chapzlock.core.asset.gltf;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Engine representation of a glTF scene node.
 *
 * <p><strong>What is a node?</strong> A glTF file organizes its content as a tree of nodes.
 * Each node has a local transform (position, rotation, scale) and optionally references
 * a mesh, camera, skin (skeleton), or light. Child nodes inherit their parent's transform —
 * if a parent moves, all its children move with it.
 *
 * <p>During scene instantiation, each GltfNodeData becomes an entity with Transform,
 * Hierarchy components. The node tree becomes the entity hierarchy.
 *
 * <p><strong>Node naming:</strong> Nodes can have names (set in Blender or other tools).
 * These names are used for entity lookup at runtime — e.g., {@code instance.entity("Player")}
 * finds the entity that was instantiated from the node named "Player" in the glTF file.
 *
 * @see GltfAsset
 */
public record GltfNodeData(
    /** Node name from glTF (used for entity lookup, e.g., "Player", "MainCamera"). May be null. */
    String name,
    /** Local translation relative to parent (default: 0,0,0). */
    Vector3f translation,
    /** Local rotation relative to parent (default: identity quaternion). */
    Quaternionf rotation,
    /** Local scale relative to parent (default: 1,1,1). */
    Vector3f scale,
    /** Index into {@link GltfAsset#meshes()}, or -1 if this node has no mesh. */
    int meshIndex,
    /** Index into GltfAsset skins list, or -1 if this node is not skinned. */
    int skinIndex,
    /** Indices of child nodes in the {@link GltfAsset#nodes()} list. */
    List<Integer> childIndices
) {
    /**
     * Computes the local transform as a 4x4 matrix (Translation × Rotation × Scale order).
     *
     * <p>This is used by the scene instantiator to set up the entity's Transform component.
     *
     * @return a new matrix representing this node's local transform
     */
    public Matrix4f toLocalMatrix() {
        return new Matrix4f()
            .translation(translation)
            .rotate(rotation)
            .scale(scale);
    }
}
