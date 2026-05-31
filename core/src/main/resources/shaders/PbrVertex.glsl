#version 330 core

// Vertex attributes (must match MeshUtil attribute locations)
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoord0;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec4 aTangent;

// Uniforms
uniform mat4 uModelMatrix;
uniform mat4 uViewMatrix;
uniform mat4 uProjectionMatrix;

// Outputs to fragment shader
out vec3 vWorldPosition;
out vec2 vTexCoord0;
out vec3 vNormal;
out mat3 vTBN;

void main() {
    vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
    vWorldPosition = worldPos.xyz;
    vTexCoord0 = aTexCoord0;

    // Transform normal to world space (use inverse-transpose for non-uniform scale)
    mat3 normalMatrix = transpose(inverse(mat3(uModelMatrix)));
    vNormal = normalize(normalMatrix * aNormal);

    // Build TBN matrix for normal mapping (tangent-bitangent-normal)
    // TBN transforms from tangent space to world space
    if (length(aTangent.xyz) > 0.0) {
        vec3 T = normalize(normalMatrix * aTangent.xyz);
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
