#version 330 core

in vec2 vTexCoord;
in vec4 vColor;

uniform sampler2D uTexture;  // optional texture
uniform bool uUseTexture;    // toggle texture usage

out vec4 FragColor;

void main()
{
    vec4 texColor = vec4(1.0); // default white
    if(uUseTexture)
    texColor = texture(uTexture, vTexCoord);

    FragColor = texColor * vColor;
}
