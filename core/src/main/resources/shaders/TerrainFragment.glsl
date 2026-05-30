#version 330 core

const int MAX_POINT_LIGHTS = 8;

in vec2 passTextureCoords;
in vec3 fragWorldPosition;
in vec3 fragNormal;
in float visibility;

out vec4 fragmentColor;

uniform sampler2D backgroundTexture;
uniform sampler2D rTexture;
uniform sampler2D bTexture;
uniform sampler2D blendMapTexture;
uniform float shineDamper;
uniform float reflectivity;
uniform vec3 skyColor;
uniform float ambientStrength;
uniform bool fogEnabled;

uniform bool hasDirLight;
uniform vec3 dirLightDirection;
uniform vec3 dirLightColor;

uniform int numPointLights;
uniform vec3 pointLightPositions[MAX_POINT_LIGHTS];
uniform vec3 pointLightColors[MAX_POINT_LIGHTS];
uniform vec3 pointLightAttenuation[MAX_POINT_LIGHTS];

uniform vec3 cameraPosition;

void main() {
    vec4 blendMapColour = texture(blendMapTexture, passTextureCoords);
    float backGroundTextureAmount = 1 - (blendMapColour.r + blendMapColour.g + blendMapColour.b);
    vec2 tiledCoords = passTextureCoords * 40.0;
    vec4 backgroundTextureColour = texture(backgroundTexture, tiledCoords) * backGroundTextureAmount;
    vec4 rTextureColour = texture(rTexture, tiledCoords) * blendMapColour.r;
    vec4 bTextureColour = texture(bTexture, tiledCoords) * blendMapColour.b;
    vec4 totalColour = backgroundTextureColour + rTextureColour + bTextureColour;

    vec3 unitNormal = normalize(fragNormal);
    vec3 viewDir = normalize(cameraPosition - fragWorldPosition);

    vec3 totalDiffuse = vec3(0.0);
    vec3 totalSpecular = vec3(0.0);

    if (hasDirLight) {
        vec3 lightDir = normalize(-dirLightDirection);
        float diff = max(dot(unitNormal, lightDir), 0.0);
        totalDiffuse += diff * dirLightColor;

        vec3 reflectDir = reflect(-lightDir, unitNormal);
        float spec = pow(max(dot(reflectDir, viewDir), 0.0), shineDamper);
        totalSpecular += spec * reflectivity * dirLightColor;
    }

    for (int i = 0; i < numPointLights; i++) {
        vec3 toLightVec = pointLightPositions[i] - fragWorldPosition;
        float distance = length(toLightVec);
        vec3 lightDir = toLightVec / distance;

        vec3 atten = pointLightAttenuation[i];
        float attFactor = 1.0 / (atten.x + atten.y * distance + atten.z * distance * distance);

        float diff = max(dot(unitNormal, lightDir), 0.0);
        totalDiffuse += diff * pointLightColors[i] * attFactor;

        vec3 reflectDir = reflect(-lightDir, unitNormal);
        float spec = pow(max(dot(reflectDir, viewDir), 0.0), shineDamper);
        totalSpecular += spec * reflectivity * pointLightColors[i] * attFactor;
    }

    vec3 ambient = vec3(ambientStrength);
    vec3 lighting = ambient + totalDiffuse;

    fragmentColor = vec4(lighting, 1.0) * totalColour + vec4(totalSpecular, 1.0);

    if (fogEnabled) {
        fragmentColor = mix(vec4(skyColor, 1.0), fragmentColor, visibility);
    }
}
