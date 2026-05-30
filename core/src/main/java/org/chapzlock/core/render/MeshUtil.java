package org.chapzlock.core.render;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.asset.RawMeshData;
import org.lwjgl.system.MemoryUtil;

import lombok.experimental.UtilityClass;

/**
 * Handles OpenGL calls related mesh resources
 */
@UtilityClass
public class MeshUtil {

    /**
     * Binds the mesh data to GPU
     *
     * @param rawMeshData
     * @return builder for the mesh
     */
    public static Mesh bindMeshDataToGpu(RawMeshData rawMeshData) {
        float[] positions = rawMeshData.getPositions();
        float[] texCoords = rawMeshData.getTexCoords();
        int[] indices = rawMeshData.getIndices();
        float[] normals = rawMeshData.getNormals();

        var mesh = Mesh.builder();
        mesh.vertexCount(indices.length);

        int vaoId = glGenVertexArrays();
        mesh.vaoId(vaoId);
        glBindVertexArray(vaoId);

        mesh.positionsVboId(bindFloatData(0, 3, positions));
        mesh.textureCoordinatesVboId(bindFloatData(1, 2, texCoords));
        if (normals != null && normals.length > 0) {
            mesh.normalsVboId(bindFloatData(2, 3, normals));
        }

        mesh.indicesVboId(bindIndexData(indices));

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        mesh.id(ResourceIdGenerator.nextId());
        return mesh.build();
    }

    private static int bindFloatData(int attrib, int size, float[] data) {
        int vbo = glGenBuffers();
        FloatBuffer buffer = MemoryUtil.memAllocFloat(data.length);
        buffer.put(data).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attrib, size, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(attrib);

        MemoryUtil.memFree(buffer);
        return vbo;
    }

    private static int bindIndexData(int[] indices) {
        int vbo = glGenBuffers();
        IntBuffer buffer = MemoryUtil.memAllocInt(indices.length);
        buffer.put(indices).flip();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        MemoryUtil.memFree(buffer);
        return vbo;
    }

    /**
     * Binds VAO and renders triangles. Vertex attribute state is stored
     * in the VAO (enabled during bindMeshDataToGpu), so only bind/draw/unbind is needed.
     */
    public static void render(Mesh mesh) {
        glBindVertexArray(mesh.getVaoId());
        glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    /**
     * Frees resources associated with the mesh from the GPU
     *
     * @param mesh
     */
    public static void delete(Mesh mesh) {
        // Disable
        glDisableVertexAttribArray(0);

        // Delete buffers
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(mesh.getPositionsVboId());
        glDeleteBuffers(mesh.getTextureCoordinatesVboId());

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glDeleteBuffers(mesh.getIndicesVboId());

        // Delete VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(mesh.getVaoId());
    }
}
