#version 330 core

// Vertex attributes (must match MeshUtil attribute locations)
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoord0;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec4 aTangent;
layout (location = 4) in ivec4 aJoints;
layout (location = 5) in vec4 aWeights;

// Uniforms
uniform mat4 uModelMatrix;
uniform mat4 uViewMatrix;
uniform mat4 uProjectionMatrix;

// Skeletal animation (GPU skinning) uniforms.
// uHasSkin is false for static meshes; when true, vertex positions/normals/tangents
// are transformed by a weighted blend of joint matrices before the model matrix.
const int MAX_JOINTS = 128;
uniform bool uHasSkin;
uniform mat4 uJointMatrices[MAX_JOINTS];

// Outputs to fragment shader
out vec3 vWorldPosition;
out vec2 vTexCoord0;
out vec3 vNormal;
out mat3 vTBN;

void main() {
    // --- GPU Skinning ---
    // For skinned meshes, blend position/normal/tangent by joint influences.
    // Each vertex stores up to 4 joint indices and corresponding weights.
    // The skin matrix is a weighted sum of the joint matrices referenced by this vertex.
    vec3 skinnedPosition = aPosition;
    vec3 skinnedNormal = aNormal;
    vec3 skinnedTangent = aTangent.xyz;

    if (uHasSkin) {
        mat4 skinMatrix = aWeights.x * uJointMatrices[aJoints.x]
                        + aWeights.y * uJointMatrices[aJoints.y]
                        + aWeights.z * uJointMatrices[aJoints.z]
                        + aWeights.w * uJointMatrices[aJoints.w];
        skinnedPosition = (skinMatrix * vec4(aPosition, 1.0)).xyz;
        skinnedNormal = mat3(skinMatrix) * aNormal;
        skinnedTangent = mat3(skinMatrix) * aTangent.xyz;
    }

    vec4 worldPos = uModelMatrix * vec4(skinnedPosition, 1.0);
    vWorldPosition = worldPos.xyz;
    vTexCoord0 = aTexCoord0;

    // Transform normal to world space (use inverse-transpose for non-uniform scale)
    mat3 normalMatrix = transpose(inverse(mat3(uModelMatrix)));
    vNormal = normalize(normalMatrix * skinnedNormal);

    // Build TBN matrix for normal mapping (tangent-bitangent-normal)
    // TBN transforms from tangent space to world space
    if (length(skinnedTangent) > 0.0) {
        vec3 T = normalize(normalMatrix * skinnedTangent);
        vec3 N = vNormal;
        // Re-orthogonalize (Gram-Schmidt) in case of floating point drift
        T = normalize(T - dot(T, N) * N);
        // Bitangent = cross(N, T) * handedness (stored in tangent.w)
        vec3 B = cross(N, T) * aTangent.w;
        vTBN = mat3(T, B, N);
    } else {
        vTBN = mat3(1.0);
    }

    gl_Position = uProjectionMatrix * uViewMatrix * worldPos;
}
