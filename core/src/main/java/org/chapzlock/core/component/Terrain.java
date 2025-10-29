package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Component for storing generic terrain information
 */
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
