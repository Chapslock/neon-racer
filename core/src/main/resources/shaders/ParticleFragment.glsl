#version 330 core

in vec2 vTexCoord;
in vec4 vColor;

out vec4 fragColor;

uniform sampler2D uTexture;

void main() {
    vec4 texSample = texture(uTexture, vTexCoord);
    fragColor = texSample * vColor;
    if (fragColor.a < 0.01) discard;
}
