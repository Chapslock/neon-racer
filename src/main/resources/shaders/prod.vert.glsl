#version 330 core

// Attributes
layout(location = 0) in vec3 aPos;      // position (x,y,z)
layout(location = 1) in vec2 aTexCoord; // texture coordinates
layout(location = 2) in vec4 aColor;    // vertex color

// Uniforms
uniform mat4 uProjection; // ortho or perspective
uniform mat4 uView;       // camera
uniform mat4 uModel;      // entity transform

// Outputs
out vec2 vTexCoord;
out vec4 vColor;

void main()
{
    gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
    vTexCoord = aTexCoord;
    vColor = aColor;
}
