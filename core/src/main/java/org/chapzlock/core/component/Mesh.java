package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An internal representation of an OpenGL mesh
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
