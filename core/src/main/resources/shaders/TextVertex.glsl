#version 330 core

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aUv;
layout(location = 2) in vec4 aColor;

uniform mat4 uProjection;
uniform mat4 uModel;

out vec2 vUv;
out vec4 vColor;

void main() {
    gl_Position = uProjection * uModel * vec4(aPosition, 0.0, 1.0);
    vUv = aUv;
    vColor = aColor;
}
