package org.chapzlock.core.asset;

/**
 * This is an internal representation of an OpenGL mesh
 */
class RawMesh {
    int vaoId;
    int positionsVboId;
    int textureCoordinatesVboId;
    int indicesVboId;
    Integer normalsVboId;
    int vertexCount;
}
