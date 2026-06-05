#version 330 core

// Per-vertex attributes (the quad geometry)
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aTexCoord;

// Per-instance attributes (one per particle)
layout(location = 3) in vec3 iPosition;
layout(location = 4) in float iSize;
layout(location = 5) in vec4 iColor;

uniform mat4 uViewMatrix;
uniform mat4 uProjectionMatrix;

out vec2 vTexCoord;
out vec4 vColor;

void main() {
    vec3 cameraRight = vec3(uViewMatrix[0][0], uViewMatrix[1][0], uViewMatrix[2][0]);
    vec3 cameraUp = vec3(uViewMatrix[0][1], uViewMatrix[1][1], uViewMatrix[2][1]);

    vec3 worldPos = iPosition
        + cameraRight * aPosition.x * iSize
        + cameraUp * aPosition.y * iSize;

    gl_Position = uProjectionMatrix * uViewMatrix * vec4(worldPos, 1.0);
    vTexCoord = aTexCoord;
    vColor = iColor;
}
