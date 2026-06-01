package org.chapzlock.core.asset.gltf;

import org.joml.Matrix4f;

/**
 * Engine representation of a glTF skin (skeleton rig definition).
 *
 * <p><strong>What is a skin?</strong> In 3D animation, a "skin" defines how a mesh deforms
 * based on a skeleton of joints (bones). Each joint is a node in the glTF scene hierarchy.
 * When joints move (via animation), the mesh vertices follow based on their bone weights.
 *
 * <p><strong>Inverse bind matrices:</strong> Each joint has an "inverse bind matrix" that
 * transforms a vertex from mesh-local space into that joint's bone-local space. At runtime,
 * the skinning formula for a vertex is:
 * <pre>
 *   skinnedPosition = sum(weight[j] * jointWorldMatrix[j] * inverseBindMatrix[j] * position)
 * </pre>
 * The inverse bind matrix captures where the joint was when the artist "bound" the mesh to
 * the skeleton. Without it, the mesh would distort incorrectly when joints move.
 *
 * <p><strong>Why node indices, not entity IDs?</strong> At load time, entities have not been
 * created yet — we only know which nodes in the glTF file are joints. During scene
 * instantiation, the instantiator creates entities for all nodes, then remaps these node
 * indices to entity IDs to create the engine's Skeleton component.
 *
 * <p><strong>Lifecycle:</strong>
 * <ol>
 *   <li>{@link GltfLoader} extracts this from the glTF file</li>
 *   <li>Scene instantiator reads {@code jointNodeIndices} and maps each to the corresponding entity ID</li>
 *   <li>A Skeleton component is created on the skinned mesh entity with entity-ID-based joint references</li>
 *   <li>This data object is then discarded</li>
 * </ol>
 *
 * @see GltfAsset
 * @see GltfNodeData
 */
public record GltfSkinData(
    /** Skin name from glTF (for debugging/logging, e.g., "Armature"). May be null. */
    String name,
    /**
     * Indices into {@link GltfAsset#nodes()} identifying which nodes are joints in this skeleton.
     *
     * <p>The order matters: joint index 0 corresponds to {@code inverseBindMatrices[0]},
     * and vertex JOINTS_0 attributes reference joints by their position in this array.
     */
    int[] jointNodeIndices,
    /**
     * One 4x4 inverse bind matrix per joint, in the same order as {@link #jointNodeIndices()}.
     *
     * <p>Each matrix transforms from mesh-local space to the joint's bone-local space at
     * bind time. These are stored in column-major order consistent with JOML and OpenGL conventions.
     */
    Matrix4f[] inverseBindMatrices
) {}
