#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 inTextureCoords;

out vec2 passTextureCoords;

void main()
{
    // Pass vertex directly to clip space
    gl_Position = vec4(position, 1.0);
    passTextureCoords = inTextureCoords;
}
