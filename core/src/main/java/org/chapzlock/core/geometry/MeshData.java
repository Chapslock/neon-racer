package org.chapzlock.core.geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class MeshData {
    private final float[] positions;
    private final float[] texCoords;
    private final int[] indices;
    private float[] normals = new float[0];
}
