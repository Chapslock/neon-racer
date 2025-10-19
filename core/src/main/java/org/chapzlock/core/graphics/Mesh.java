package org.chapzlock.core.graphics;

import static org.lwjgl.opengl.GL30.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL30.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL30.GL_FLOAT;
import static org.lwjgl.opengl.GL30.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30.GL_TRIANGLES;
import static org.lwjgl.opengl.GL30.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL30.glBindBuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glBufferData;
import static org.lwjgl.opengl.GL30.glDeleteBuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glDrawElements;
import static org.lwjgl.opengl.GL30.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glGenBuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glVertexAttribPointer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.chapzlock.core.component.Component;
import org.chapzlock.core.geometry.MeshData;
import org.lwjgl.system.MemoryUtil;

/**
 * Generic class for representing a mesh that OpenGL can draw
 */
public class Mesh implements Component {

    private final int vaoId;
    private final int vertexCount;

    private final int positionsVboId;
    private final int textureCoordinatesVboId;
    private final int indicesVboId;
    private Integer normalsVboId;

    public Mesh(MeshData meshData) {
        this(meshData.getPositions(), meshData.getTexCoords(), meshData.getIndices(), meshData.getNormals());
    }

    public Mesh(float[] positions, float[] texCoords, int[] indices, float[] normals) {
        vertexCount = indices.length;
        // Create VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        positionsVboId = bindDataToAttributeList(0, 3, positions);
        textureCoordinatesVboId = bindDataToAttributeList(1, 2, texCoords);
        if (normals.length != 0) {
            normalsVboId = bindDataToAttributeList(2, 3, normals);
        }
        indicesVboId = bindIndicesBuffer(indices);

        // Unbind VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        // Unbind VAO (EBO remains bound to VAO)
        glBindVertexArray(0);
    }

    private int bindDataToAttributeList(int attributeIndex, int numberOfValuesPerVertex, float[] data) {
        int vboId = glGenBuffers();
        FloatBuffer dataBuffer = MemoryUtil.memAllocFloat(data.length);
        dataBuffer.put(data).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, dataBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeIndex, numberOfValuesPerVertex, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(attributeIndex);

        MemoryUtil.memFree(dataBuffer);
        return vboId;
    }

    private int bindIndicesBuffer(int[] indices) {
        final int vboId = glGenBuffers();
        IntBuffer idxBuffer = MemoryUtil.memAllocInt(indices.length);
        idxBuffer.put(indices).flip();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuffer, GL_STATIC_DRAW);

        MemoryUtil.memFree(idxBuffer);
        return vboId;
    }

    public void render() {
        glBindVertexArray(vaoId);
        glEnableVertexAttribArray(0); // positions
        glEnableVertexAttribArray(1); // texCoords
        if (normalsVboId != null) {
            glEnableVertexAttribArray(2); //normals
        }

        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);


        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        if (normalsVboId != null) {
            glDisableVertexAttribArray(2);
        }
        glBindVertexArray(0);
    }

    public void cleanup() {
        // Disable
        glDisableVertexAttribArray(0);

        // Delete buffers
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(positionsVboId);
        glDeleteBuffers(textureCoordinatesVboId);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glDeleteBuffers(indicesVboId);

        // Delete VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }
}
