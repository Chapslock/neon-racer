package org.chapzlock.core.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * CPU-side mesh data before GPU upload.
 *
 * <p>Contains all vertex attributes that a mesh can have. Not all attributes are required —
 * use the {@code has*()} methods to check which are present. The GPU upload code
 * ({@link org.chapzlock.core.render.MeshUtil}) conditionally enables vertex attributes
 * based on what data is available.
 *
 * <p><strong>Vertex attribute layout (matches shader locations):</strong>
 * <ul>
 *   <li>Location 0: position (vec3) — always required, 3 floats per vertex (x, y, z)</li>
 *   <li>Location 1: texCoord0 (vec2) — primary UV coordinates, 2 floats per vertex</li>
 *   <li>Location 2: normal (vec3) — surface normal for lighting calculations</li>
 *   <li>Location 3: tangent (vec4) — for normal mapping, w component = handedness (+1 or -1)</li>
 *   <li>Location 4: joints (ivec4) — bone indices for skeletal animation, 4 ints per vertex</li>
 *   <li>Location 5: weights (vec4) — bone weights for skeletal animation, 4 floats per vertex (sum to 1.0)</li>
 *   <li>Location 6: color (vec4) — per-vertex color (RGBA), 4 floats per vertex</li>
 *   <li>Location 7: texCoord1 (vec2) — secondary UV set (some assets use two UV sets)</li>
 * </ul>
 *
 * <p><strong>What are tangents?</strong> Tangent vectors are needed for normal mapping — a technique
 * that adds surface detail (bumps, scratches) without extra geometry. The tangent + bitangent + normal
 * form a coordinate system at each vertex called "tangent space" that lets the shader interpret
 * the normal map correctly.
 *
 * <p><strong>What are joints and weights?</strong> For skeletal animation, each vertex is influenced
 * by up to 4 bones (joints). The joints array stores WHICH bones affect this vertex (as indices),
 * and the weights array stores HOW MUCH each bone influences it (should sum to 1.0).
 *
 * @see org.chapzlock.core.render.MeshUtil#bindMeshDataToGpu(RawMeshData)
 */
@Getter
@Builder
@AllArgsConstructor
public class RawMeshData {
    private final float[] positions;
    private final float[] texCoords;
    private final int[] indices;
    @Builder.Default
    private float[] normals = new float[0];
    @Builder.Default
    private float[] tangents = new float[0];
    @Builder.Default
    private int[] joints = new int[0];
    @Builder.Default
    private float[] weights = new float[0];
    @Builder.Default
    private float[] colors = new float[0];
    @Builder.Default
    private float[] texCoords1 = new float[0];

    /**
     * Legacy constructor for backwards compatibility with existing code.
     * Creates a mesh with only positions, texture coordinates, and indices.
     */
    public RawMeshData(float[] positions, float[] texCoords, int[] indices) {
        this(positions, texCoords, indices, new float[0], new float[0], new int[0], new float[0], new float[0], new float[0]);
    }

    /**
     * Legacy constructor with normals for backwards compatibility.
     */
    public RawMeshData(float[] positions, float[] texCoords, int[] indices, float[] normals) {
        this(positions, texCoords, indices, normals, new float[0], new int[0], new float[0], new float[0], new float[0]);
    }

    /** True if normal data is present. */
    public boolean hasNormals() { return normals != null && normals.length > 0; }

    /** True if tangent data is present (vec4 per vertex: xyz = tangent direction, w = handedness). */
    public boolean hasTangents() { return tangents != null && tangents.length > 0; }

    /** True if joint indices are present (4 bone indices per vertex for skeletal animation). */
    public boolean hasJoints() { return joints != null && joints.length > 0; }

    /** True if bone weights are present (4 weights per vertex, should sum to 1.0). */
    public boolean hasWeights() { return weights != null && weights.length > 0; }

    /** True if per-vertex colors are present (RGBA, 4 floats per vertex). */
    public boolean hasColors() { return colors != null && colors.length > 0; }

    /** True if a secondary UV set is present. */
    public boolean hasTexCoords1() { return texCoords1 != null && texCoords1.length > 0; }
}
