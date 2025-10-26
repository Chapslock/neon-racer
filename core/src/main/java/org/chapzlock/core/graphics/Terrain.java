package org.chapzlock.core.graphics;

import org.chapzlock.core.component.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Terrain implements Component {

    public static final float DEFAULT_SIZE = 800;
    public static final int DEFAULT_VERTEX_COUNT = 128;
    /**
     * How many repeats per world unit of a Texture
     */
    private final float tileScale;
    private float size = DEFAULT_SIZE;
    private int vertexCount = DEFAULT_VERTEX_COUNT;
}
