#version 330 core

in vec2 vUv;
in vec4 vColor;

uniform sampler2D uAtlas;
uniform float uDistanceRange;
uniform float uOutlineWidth;
uniform vec4 uOutlineColor;

out vec4 fragColor;

float median(vec3 msdf) {
    return max(min(msdf.r, msdf.g), min(max(msdf.r, msdf.g), msdf.b));
}

float screenPxAlpha(float distance) {
    return smoothstep(-0.5, 0.5, distance / fwidth(distance));
}

void main() {
    vec3 msdfSample = texture(uAtlas, vUv).rgb;
    float dist = uDistanceRange * (median(msdfSample) - 0.5);
    float textAlpha = screenPxAlpha(dist);

    vec4 result = vec4(vColor.rgb, vColor.a * textAlpha);

    if (uOutlineWidth > 0.0) {
        float outlineAlpha = screenPxAlpha(dist + uDistanceRange * uOutlineWidth);
        vec4 outline = vec4(uOutlineColor.rgb, uOutlineColor.a * outlineAlpha);
        result = mix(outline, result, textAlpha);
    }

    if (result.a < 0.01) discard;
    fragColor = result;
}
