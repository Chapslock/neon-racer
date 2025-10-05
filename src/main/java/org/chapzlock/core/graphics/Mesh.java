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

import org.lwjgl.system.MemoryUtil;

/**
 * Generic class for representing a mesh that OpenGL can draw
 */
public class Mesh {

    private final int vaoId;
    private final int vertexCount;

    private final int posVboId;
    private final int texVboId;
    private final int idxVboId;

    public Mesh(float[] positions, float[] texCoords, int[] indices) {
        vertexCount = indices.length;

        // Create VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // ==== Positions VBO ====
        posVboId = glGenBuffers();
        FloatBuffer posBuffer = MemoryUtil.memAllocFloat(positions.length);
        posBuffer.put(positions).flip();

        glBindBuffer(GL_ARRAY_BUFFER, posVboId);
        glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
        // location = 0 in shader (vec3 aPos or vec2 aPos for 2D)
        glVertexAttribPointer(Graphics.SHADER_POSITION_ATTRIBUTE_INDEX, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(Graphics.SHADER_POSITION_ATTRIBUTE_INDEX);

        MemoryUtil.memFree(posBuffer);

        // ==== Texture Coordinates VBO ====
        texVboId = glGenBuffers();
        FloatBuffer texBuffer = MemoryUtil.memAllocFloat(texCoords.length);
        texBuffer.put(texCoords).flip();

        glBindBuffer(GL_ARRAY_BUFFER, texVboId);
        glBufferData(GL_ARRAY_BUFFER, texBuffer, GL_STATIC_DRAW);
        // location = 1 in shader (vec2 aTexCoord)
        glVertexAttribPointer(Graphics.SHADER_TEXTURE_COORDINATES_ATTRIBUTE_INDEX, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(Graphics.SHADER_TEXTURE_COORDINATES_ATTRIBUTE_INDEX);

        MemoryUtil.memFree(texBuffer);

        // ==== Indices (EBO) ====
        idxVboId = glGenBuffers();
        IntBuffer idxBuffer = MemoryUtil.memAllocInt(indices.length);
        idxBuffer.put(indices).flip();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, idxVboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuffer, GL_STATIC_DRAW);

        MemoryUtil.memFree(idxBuffer);

        // Unbind VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        // Unbind VAO (EBO remains bound to VAO)
        glBindVertexArray(0);
    }

    public void render() {
        glBindVertexArray(vaoId);
        glEnableVertexAttribArray(Graphics.SHADER_POSITION_ATTRIBUTE_INDEX); // positions
        glEnableVertexAttribArray(Graphics.SHADER_TEXTURE_COORDINATES_ATTRIBUTE_INDEX); // texCoords

        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);

        glDisableVertexAttribArray(Graphics.SHADER_POSITION_ATTRIBUTE_INDEX);
        glDisableVertexAttribArray(Graphics.SHADER_TEXTURE_COORDINATES_ATTRIBUTE_INDEX);
        glBindVertexArray(0);
    }

    public void cleanup() {
        // Disable
        glDisableVertexAttribArray(0);

        // Delete buffers
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(posVboId);
        glDeleteBuffers(texVboId);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glDeleteBuffers(idxVboId);

        // Delete VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }
}
