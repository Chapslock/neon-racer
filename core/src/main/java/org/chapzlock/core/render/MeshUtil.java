package org.chapzlock.core.render;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_INT;
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
import static org.lwjgl.opengl.GL30.glVertexAttribIPointer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.chapzlock.core.asset.RawMeshData;
import org.chapzlock.core.component.Mesh;
import org.lwjgl.system.MemoryUtil;

import lombok.experimental.UtilityClass;

/**
 * Handles OpenGL calls for mesh GPU resource management.
 *
 * <p>Uploads vertex data to the GPU using Vertex Array Objects (VAOs) and
 * Vertex Buffer Objects (VBOs). Each vertex attribute gets its own VBO,
 * and the VAO remembers which attributes are enabled — so during rendering
 * we just bind the VAO and issue a draw call.
 *
 * <p><strong>Vertex attribute locations (must match shader layout qualifiers):</strong>
 * <ul>
 *   <li>0: position (vec3) — always present</li>
 *   <li>1: texCoord0 (vec2) — always present</li>
 *   <li>2: normal (vec3) — optional</li>
 *   <li>3: tangent (vec4) — optional, for normal mapping</li>
 *   <li>4: joints (ivec4) — optional, integer attribute for skeletal animation</li>
 *   <li>5: weights (vec4) — optional, for skeletal animation</li>
 *   <li>6: color (vec4) — optional, per-vertex color</li>
 *   <li>7: texCoord1 (vec2) — optional, secondary UV set</li>
 * </ul>
 *
 * <p><strong>Why integer attributes are special:</strong> Joint indices must stay as integers
 * in the shader (used to index into a bone matrix array). Normal float attributes use
 * {@code glVertexAttribPointer} which can convert types, but integer attributes must use
 * {@code glVertexAttribIPointer} (note the "I") to pass raw integers to the shader.
 */
@UtilityClass
public class MeshUtil {

    /** Shader attribute location for vertex positions. */
    public static final int ATTRIB_POSITION = 0;
    /** Shader attribute location for primary texture coordinates. */
    public static final int ATTRIB_TEXCOORD0 = 1;
    /** Shader attribute location for vertex normals. */
    public static final int ATTRIB_NORMAL = 2;
    /** Shader attribute location for tangent vectors (vec4, w = handedness). */
    public static final int ATTRIB_TANGENT = 3;
    /** Shader attribute location for joint/bone indices (ivec4). */
    public static final int ATTRIB_JOINTS = 4;
    /** Shader attribute location for bone weights (vec4). */
    public static final int ATTRIB_WEIGHTS = 5;
    /** Shader attribute location for per-vertex color (vec4). */
    public static final int ATTRIB_COLOR = 6;
    /** Shader attribute location for secondary texture coordinates. */
    public static final int ATTRIB_TEXCOORD1 = 7;

    /**
     * Uploads mesh data to the GPU, creating a VAO with VBOs for each present attribute.
     *
     * <p>Only attributes with data in the RawMeshData are uploaded and enabled in the VAO.
     * Missing attributes will receive default values in the shader (typically zero).
     *
     * @param rawMeshData the CPU-side mesh data to upload
     * @return a Mesh component holding all GPU resource IDs
     */
    public static Mesh bindMeshDataToGpu(RawMeshData rawMeshData) {
        var mesh = Mesh.builder();
        mesh.vertexCount(rawMeshData.getIndices().length);

        int vaoId = glGenVertexArrays();
        mesh.vaoId(vaoId);
        glBindVertexArray(vaoId);

        // Required attributes
        mesh.positionsVboId(bindFloatData(ATTRIB_POSITION, 3, rawMeshData.getPositions()));
        mesh.textureCoordinatesVboId(bindFloatData(ATTRIB_TEXCOORD0, 2, rawMeshData.getTexCoords()));

        // Optional attributes — only uploaded if present in the mesh data
        if (rawMeshData.hasNormals()) {
            mesh.normalsVboId(bindFloatData(ATTRIB_NORMAL, 3, rawMeshData.getNormals()));
        }
        if (rawMeshData.hasTangents()) {
            mesh.tangentsVboId(bindFloatData(ATTRIB_TANGENT, 4, rawMeshData.getTangents()));
        }
        if (rawMeshData.hasJoints()) {
            mesh.jointsVboId(bindIntData(ATTRIB_JOINTS, 4, rawMeshData.getJoints()));
        }
        if (rawMeshData.hasWeights()) {
            mesh.weightsVboId(bindFloatData(ATTRIB_WEIGHTS, 4, rawMeshData.getWeights()));
        }
        if (rawMeshData.hasColors()) {
            mesh.colorsVboId(bindFloatData(ATTRIB_COLOR, 4, rawMeshData.getColors()));
        }
        if (rawMeshData.hasTexCoords1()) {
            mesh.texCoords1VboId(bindFloatData(ATTRIB_TEXCOORD1, 2, rawMeshData.getTexCoords1()));
        }

        mesh.indicesVboId(bindIndexData(rawMeshData.getIndices()));

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        mesh.id(ResourceIdGenerator.nextId());
        return mesh.build();
    }

    /**
     * Uploads float vertex attribute data to a new VBO and enables the attribute in the VAO.
     *
     * @param attrib the shader attribute location (0-7)
     * @param size number of components per vertex (e.g., 3 for vec3, 4 for vec4)
     * @param data the float array to upload
     * @return the OpenGL VBO ID
     */
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

    /**
     * Uploads integer vertex attribute data to a new VBO using glVertexAttribIPointer.
     *
     * <p>Unlike float attributes, integer attributes must use {@code glVertexAttribIPointer}
     * (with the "I") to ensure values arrive as integers in the shader. This is essential
     * for joint/bone indices which are used to index into a uniform array of bone matrices.
     *
     * @param attrib the shader attribute location
     * @param size number of components per vertex (e.g., 4 for ivec4)
     * @param data the int array to upload
     * @return the OpenGL VBO ID
     */
    private static int bindIntData(int attrib, int size, int[] data) {
        int vbo = glGenBuffers();
        IntBuffer buffer = MemoryUtil.memAllocInt(data.length);
        buffer.put(data).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        glVertexAttribIPointer(attrib, size, GL_INT, 0, 0);
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
     * Binds the mesh's VAO and issues a draw call for all its triangles.
     *
     * <p>The VAO remembers which attributes are enabled and how they're configured
     * (set up during {@link #bindMeshDataToGpu}), so we just bind and draw.
     *
     * @param mesh the mesh to render
     */
    public static void render(Mesh mesh) {
        glBindVertexArray(mesh.getVaoId());
        glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    /**
     * Frees all GPU resources (VBOs and VAO) associated with a mesh.
     *
     * <p>Called by MeshSystem when a mesh's reference count reaches zero (all scenes
     * using it have been unloaded) or during engine shutdown.
     *
     * @param mesh the mesh whose GPU resources should be freed
     */
    public static void delete(Mesh mesh) {
        glDisableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(mesh.getPositionsVboId());
        glDeleteBuffers(mesh.getTextureCoordinatesVboId());
        if (mesh.getNormalsVboId() != null) glDeleteBuffers(mesh.getNormalsVboId());
        if (mesh.getTangentsVboId() != null) glDeleteBuffers(mesh.getTangentsVboId());
        if (mesh.getJointsVboId() != null) glDeleteBuffers(mesh.getJointsVboId());
        if (mesh.getWeightsVboId() != null) glDeleteBuffers(mesh.getWeightsVboId());
        if (mesh.getColorsVboId() != null) glDeleteBuffers(mesh.getColorsVboId());
        if (mesh.getTexCoords1VboId() != null) glDeleteBuffers(mesh.getTexCoords1VboId());

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glDeleteBuffers(mesh.getIndicesVboId());

        glBindVertexArray(0);
        glDeleteVertexArrays(mesh.getVaoId());
    }
}
