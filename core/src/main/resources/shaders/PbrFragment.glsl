#version 330 core

// Constants
const float PI = 3.14159265359;
const int MAX_POINT_LIGHTS = 8;

// Inputs from vertex shader
in vec3 vWorldPosition;
in vec2 vTexCoord0;
in vec3 vNormal;
in mat3 vTBN;

// Material uniforms
uniform vec4 uBaseColorFactor;
uniform float uMetallicFactor;
uniform float uRoughnessFactor;
uniform vec3 uEmissiveFactor;
uniform float uAlphaCutoff;

// Texture samplers
uniform sampler2D uBaseColorMap;
uniform sampler2D uMetallicRoughnessMap;
uniform sampler2D uNormalMap;
uniform sampler2D uOcclusionMap;
uniform sampler2D uEmissiveMap;

// Feature flags (which textures are bound)
uniform bool uHasBaseColorMap;
uniform bool uHasMetallicRoughnessMap;
uniform bool uHasNormalMap;
uniform bool uHasOcclusionMap;
uniform bool uHasEmissiveMap;
uniform int uAlphaMode; // 0=OPAQUE, 1=MASK, 2=BLEND

// Lighting uniforms
uniform vec3 uCameraPosition;
uniform float uAmbientStrength;

// Directional light
uniform bool uHasDirLight;
uniform vec3 uDirLightDirection;
uniform vec3 uDirLightColor;

// Point lights
uniform int uNumPointLights;
uniform vec3 uPointLightPositions[MAX_POINT_LIGHTS];
uniform vec3 uPointLightColors[MAX_POINT_LIGHTS];
uniform vec3 uPointLightAttenuation[MAX_POINT_LIGHTS]; // (constant, linear, quadratic)

// Output
out vec4 fragColor;

// --- PBR Functions ---

// Normal Distribution Function (GGX/Trowbridge-Reitz)
// Models the statistical distribution of microfacet orientations.
// Higher roughness = more facets face away from the half vector = wider specular highlight.
float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;
    return a2 / max(denom, 0.0001);
}

// Geometry function (Smith's method with Schlick-GGX)
// Models self-shadowing of microfacets — at grazing angles, rough surfaces
// block more light because microfacets occlude each other.
float geometrySchlickGGX(float NdotV, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
}

float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    return geometrySchlickGGX(NdotV, roughness) * geometrySchlickGGX(NdotL, roughness);
}

// Fresnel equation (Schlick approximation)
// At steep angles you see the surface color; at grazing angles, all surfaces
// become mirror-like. This is why water looks transparent looking down but
// reflective at the horizon.
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// Compute Cook-Torrance BRDF for a single light
vec3 computeLight(vec3 N, vec3 V, vec3 L, vec3 lightColor,
                  vec3 albedo, float metallic, float roughness) {
    vec3 H = normalize(V + L);

    // F0 = surface reflectance at zero incidence
    // Dielectrics (non-metals): ~0.04. Metals: use albedo as F0.
    vec3 F0 = mix(vec3(0.04), albedo, metallic);

    // Cook-Torrance specular BRDF
    float D = distributionGGX(N, H, roughness);
    float G = geometrySmith(N, V, L, roughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

    vec3 numerator = D * G * F;
    float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.0001;
    vec3 specular = numerator / denominator;

    // Energy conservation: light is either reflected (specular) or refracted (diffuse)
    vec3 kS = F;
    vec3 kD = (vec3(1.0) - kS) * (1.0 - metallic); // metals have no diffuse

    float NdotL = max(dot(N, L), 0.0);
    return (kD * albedo / PI + specular) * lightColor * NdotL;
}

void main() {
    // Sample base color
    vec4 baseColor = uBaseColorFactor;
    if (uHasBaseColorMap) {
        baseColor *= texture(uBaseColorMap, vTexCoord0);
    }

    // Alpha handling
    if (uAlphaMode == 1 && baseColor.a < uAlphaCutoff) {
        discard;
    }

    vec3 albedo = baseColor.rgb;

    // Sample metallic-roughness
    float metallic = uMetallicFactor;
    float roughness = uRoughnessFactor;
    if (uHasMetallicRoughnessMap) {
        vec4 mr = texture(uMetallicRoughnessMap, vTexCoord0);
        metallic *= mr.b;   // Blue channel = metallic
        roughness *= mr.g;  // Green channel = roughness
    }
    roughness = clamp(roughness, 0.04, 1.0); // prevent division by zero in GGX

    // Determine normal (world-space)
    vec3 N;
    if (uHasNormalMap) {
        // Sample tangent-space normal and transform to world space via TBN matrix
        vec3 tangentNormal = texture(uNormalMap, vTexCoord0).rgb * 2.0 - 1.0;
        N = normalize(vTBN * tangentNormal);
    } else {
        N = normalize(vNormal);
    }

    vec3 V = normalize(uCameraPosition - vWorldPosition);

    // Accumulate lighting
    vec3 Lo = vec3(0.0);

    // Directional light
    if (uHasDirLight) {
        vec3 L = normalize(-uDirLightDirection);
        Lo += computeLight(N, V, L, uDirLightColor, albedo, metallic, roughness);
    }

    // Point lights
    for (int i = 0; i < uNumPointLights; i++) {
        vec3 L = uPointLightPositions[i] - vWorldPosition;
        float distance = length(L);
        L = normalize(L);

        // Attenuation
        vec3 att = uPointLightAttenuation[i];
        float attenuation = 1.0 / (att.x + att.y * distance + att.z * distance * distance);

        Lo += computeLight(N, V, L, uPointLightColors[i] * attenuation,
                          albedo, metallic, roughness);
    }

    // Ambient (simple constant for now — IBL can replace this later)
    vec3 ambient = uAmbientStrength * albedo;
    if (uHasOcclusionMap) {
        float ao = texture(uOcclusionMap, vTexCoord0).r;
        ambient *= ao;
    }

    vec3 color = ambient + Lo;

    // Emissive
    vec3 emissive = uEmissiveFactor;
    if (uHasEmissiveMap) {
        emissive *= texture(uEmissiveMap, vTexCoord0).rgb;
    }
    color += emissive;

    // Tone mapping (Reinhard) and gamma correction
    color = color / (color + vec3(1.0)); // Reinhard tone mapping
    color = pow(color, vec3(1.0 / 2.2)); // Gamma correction

    fragColor = vec4(color, baseColor.a);
}
