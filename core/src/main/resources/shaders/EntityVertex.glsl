#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 inTextureCoords;
layout(location = 2) in vec3 normal;

out vec2 passTextureCoords;
out vec3 fragWorldPosition;
out vec3 fragNormal;
out float visibility;

uniform mat4 transformationMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform float fogDensity;
uniform float fogGradient;
uniform bool fogEnabled;

void main() {
    vec4 worldPosition = transformationMatrix * vec4(position, 1.0);
    vec4 positionRelativeToCamera = viewMatrix * worldPosition;
    gl_Position = projectionMatrix * positionRelativeToCamera;

    passTextureCoords = inTextureCoords;
    fragWorldPosition = worldPosition.xyz;
    fragNormal = (transformationMatrix * vec4(normal, 0.0)).xyz;

    if (fogEnabled) {
        float distanceFromCamera = length(positionRelativeToCamera.xyz);
        visibility = exp(-pow(distanceFromCamera * fogDensity, fogGradient));
        visibility = clamp(visibility, 0.0, 1.0);
    } else {
        visibility = 1.0;
    }
}
