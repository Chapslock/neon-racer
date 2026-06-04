package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GPU resource handle for a mesh — stores OpenGL buffer IDs for vertex data.
 *
 * <p><strong>What is a VAO/VBO?</strong> When mesh data is uploaded to the GPU, it's stored in
 * Vertex Buffer Objects (VBOs) — one per attribute type (positions, normals, etc.). A Vertex
 * Array Object (VAO) remembers which VBOs are active and how they're configured, so during
 * rendering we just bind the VAO and issue a draw call.
 *
 * <p>Each {@code Integer} field (nullable) represents an optional VBO. If null, that vertex
 * attribute is not present in this mesh and its shader location will receive default values.
 *
 * <p>Created by {@link org.chapzlock.core.render.MeshRegistry#load(String)}.
 * Reference-counted — retained on scene load, released on scene unload.
 *
 * @see org.chapzlock.core.render.MeshUtil#bindMeshDataToGpu
 * @see org.chapzlock.core.asset.RawMeshData
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mesh implements Component {
    /** Internal resource ID for reference counting. */
    private int id;
    /** OpenGL Vertex Array Object ID — the "container" that remembers VBO configuration. */
    private int vaoId;
    /** VBO for vertex positions (vec3, location 0). Always present. */
    private int positionsVboId;
    /** VBO for primary texture coordinates (vec2, location 1). Always present. */
    private int textureCoordinatesVboId;
    /** VBO for index buffer (element array buffer). Always present. */
    private int indicesVboId;
    /** VBO for normals (vec3, location 2). Null if mesh has no normals. */
    private Integer normalsVboId;
    /** VBO for tangent vectors (vec4, location 3). Null if no normal mapping. */
    private Integer tangentsVboId;
    /** VBO for joint/bone indices (ivec4, location 4). Null if not skinned. */
    private Integer jointsVboId;
    /** VBO for bone weights (vec4, location 5). Null if not skinned. */
    private Integer weightsVboId;
    /** VBO for per-vertex colors (vec4, location 6). Null if no vertex colors. */
    private Integer colorsVboId;
    /** VBO for secondary UV set (vec2, location 7). Null if only one UV set. */
    private Integer texCoords1VboId;
    /** Number of indices to draw (used in glDrawElements). */
    private int vertexCount;
}
