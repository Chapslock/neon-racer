#version 330 core

//In variable data comes from VAO-s
layout(location = 0) in vec3 position;
layout(location = 1) in vec2 inTextureCoords;
layout(location = 2) in vec3 normal;

out vec2 passTextureCoords;
out vec3 surfaceNormal;
out vec3 toLightVector;
out vec3 toCameraVector;
out float visibility;

uniform mat4 transformationMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform vec3 lightPosition;
uniform float fogDensity;
uniform float fogGradient;

void main()
{
    vec4 worldPosition = transformationMatrix * vec4(position, 1.0);
    vec4 positionRelativeToCamera = viewMatrix * worldPosition;
    gl_Position = projectionMatrix * positionRelativeToCamera;
    passTextureCoords = inTextureCoords;
    surfaceNormal = (transformationMatrix * vec4(normal, 0.0)).xyz;
    toLightVector = lightPosition - worldPosition.xyz;
    toCameraVector = (inverse(viewMatrix) * vec4(0, 0, 0, 1)).xyz - worldPosition.xyz;

    float distanceFromCamera = length(positionRelativeToCamera.xyz);
    visibility = exp(-pow(distanceFromCamera*fogDensity, fogGradient));
    visibility = clamp(visibility, 0.0, 1.0);
}
