#version 330 core

in vec2 passTextureCoords;
in vec3 surfaceNormal;
in vec3 toLightVector;

out vec4 fragmentColor;

uniform sampler2D textureSampler;
uniform vec3 lightColor;

void main()
{
    vec3 unitNormal = normalize(surfaceNormal);
    vec3 unitLight = normalize(toLightVector);
    float dotProduct = dot(unitNormal, unitLight);
    float brightness = max(dotProduct, 0.0);
    //vec3 diffuse = brightness * lightColor;
    //TODO: Charl LightColor is not reaching here for some reason
    vec3 diffuse = brightness * vec3(1, 1, 1);
    fragmentColor = vec4(diffuse, 1.0) * texture(textureSampler, passTextureCoords);
}
