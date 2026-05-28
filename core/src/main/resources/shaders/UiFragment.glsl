#version 330 core

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform vec4 uTintColor;

void main() {
    vec4 tex = texture(uTexture, vTexCoord);
    fragColor = tex * uTintColor;
    if (fragColor.a < 0.01) discard;
}
