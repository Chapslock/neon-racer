package org.chapzlock.core.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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

    /**
     * Combines multiple meshes into a single mesh by concatenating vertex/index buffers.
     *
     * <p>Index values are adjusted so that each mesh's indices correctly reference
     * vertices within the combined positions array. The offset for mesh N equals the
     * total vertex count of all preceding meshes (vertex count = positions.length / 3).
     *
     * <p>Optional attributes (normals, tangents, joints, weights, colors, texCoords1) are
     * only included in the result if ALL input meshes have them. If any mesh is missing
     * an optional attribute, it is omitted from the merged result.
     *
     * @param meshes non-empty list of meshes to combine
     * @return a single merged mesh
     * @throws IllegalArgumentException if the list is empty
     */
    public static RawMeshData merge(List<RawMeshData> meshes) {
        if (meshes.isEmpty()) {
            throw new IllegalArgumentException("Cannot merge an empty list of meshes");
        }
        if (meshes.size() == 1) {
            return meshes.get(0);
        }

        // Calculate total sizes
        int totalPositions = 0;
        int totalTexCoords = 0;
        int totalIndices = 0;
        int totalNormals = 0;
        int totalTangents = 0;
        int totalJoints = 0;
        int totalWeights = 0;
        int totalColors = 0;
        int totalTexCoords1 = 0;

        boolean allHaveNormals = true;
        boolean allHaveTangents = true;
        boolean allHaveJoints = true;
        boolean allHaveWeights = true;
        boolean allHaveColors = true;
        boolean allHaveTexCoords1 = true;

        for (RawMeshData mesh : meshes) {
            totalPositions += mesh.positions.length;
            totalTexCoords += mesh.texCoords.length;
            totalIndices += mesh.indices.length;

            if (mesh.hasNormals()) {
                totalNormals += mesh.normals.length;
            } else {
                allHaveNormals = false;
            }
            if (mesh.hasTangents()) {
                totalTangents += mesh.tangents.length;
            } else {
                allHaveTangents = false;
            }
            if (mesh.hasJoints()) {
                totalJoints += mesh.joints.length;
            } else {
                allHaveJoints = false;
            }
            if (mesh.hasWeights()) {
                totalWeights += mesh.weights.length;
            } else {
                allHaveWeights = false;
            }
            if (mesh.hasColors()) {
                totalColors += mesh.colors.length;
            } else {
                allHaveColors = false;
            }
            if (mesh.hasTexCoords1()) {
                totalTexCoords1 += mesh.texCoords1.length;
            } else {
                allHaveTexCoords1 = false;
            }
        }

        // Allocate merged arrays
        float[] mergedPositions = new float[totalPositions];
        float[] mergedTexCoords = new float[totalTexCoords];
        int[] mergedIndices = new int[totalIndices];
        float[] mergedNormals = allHaveNormals ? new float[totalNormals] : new float[0];
        float[] mergedTangents = allHaveTangents ? new float[totalTangents] : new float[0];
        int[] mergedJoints = allHaveJoints ? new int[totalJoints] : new int[0];
        float[] mergedWeights = allHaveWeights ? new float[totalWeights] : new float[0];
        float[] mergedColors = allHaveColors ? new float[totalColors] : new float[0];
        float[] mergedTexCoords1 = allHaveTexCoords1 ? new float[totalTexCoords1] : new float[0];

        // Copy data
        int posOffset = 0;
        int texOffset = 0;
        int idxOffset = 0;
        int normOffset = 0;
        int tanOffset = 0;
        int jointOffset = 0;
        int weightOffset = 0;
        int colorOffset = 0;
        int tc1Offset = 0;
        int vertexOffset = 0; // running vertex count for index adjustment

        for (RawMeshData mesh : meshes) {
            System.arraycopy(mesh.positions, 0, mergedPositions, posOffset, mesh.positions.length);
            posOffset += mesh.positions.length;

            System.arraycopy(mesh.texCoords, 0, mergedTexCoords, texOffset, mesh.texCoords.length);
            texOffset += mesh.texCoords.length;

            // Copy indices with offset adjustment
            for (int i = 0; i < mesh.indices.length; i++) {
                mergedIndices[idxOffset + i] = mesh.indices[i] + vertexOffset;
            }
            idxOffset += mesh.indices.length;

            if (allHaveNormals) {
                System.arraycopy(mesh.normals, 0, mergedNormals, normOffset, mesh.normals.length);
                normOffset += mesh.normals.length;
            }
            if (allHaveTangents) {
                System.arraycopy(mesh.tangents, 0, mergedTangents, tanOffset, mesh.tangents.length);
                tanOffset += mesh.tangents.length;
            }
            if (allHaveJoints) {
                System.arraycopy(mesh.joints, 0, mergedJoints, jointOffset, mesh.joints.length);
                jointOffset += mesh.joints.length;
            }
            if (allHaveWeights) {
                System.arraycopy(mesh.weights, 0, mergedWeights, weightOffset, mesh.weights.length);
                weightOffset += mesh.weights.length;
            }
            if (allHaveColors) {
                System.arraycopy(mesh.colors, 0, mergedColors, colorOffset, mesh.colors.length);
                colorOffset += mesh.colors.length;
            }
            if (allHaveTexCoords1) {
                System.arraycopy(mesh.texCoords1, 0, mergedTexCoords1, tc1Offset, mesh.texCoords1.length);
                tc1Offset += mesh.texCoords1.length;
            }

            vertexOffset += mesh.positions.length / 3;
        }

        return RawMeshData.builder()
            .positions(mergedPositions)
            .texCoords(mergedTexCoords)
            .indices(mergedIndices)
            .normals(mergedNormals)
            .tangents(mergedTangents)
            .joints(mergedJoints)
            .weights(mergedWeights)
            .colors(mergedColors)
            .texCoords1(mergedTexCoords1)
            .build();
    }
}
