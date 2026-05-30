package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GPU resource handle for a mesh (vertex buffers + VAO).
 *
 * <p>Created by {@link org.chapzlock.core.render.MeshSystem#load(String)}.
 * Reference-counted — retained on scene load, released on scene unload.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mesh implements Component {
    /**
     * Internal ID of the mesh
     */
    private int id;
    private int vaoId;
    private int positionsVboId;
    private int textureCoordinatesVboId;
    private int indicesVboId;
    private Integer normalsVboId;
    private int vertexCount;
}
