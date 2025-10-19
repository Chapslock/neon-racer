package org.chapzlock.core.component;

import org.chapzlock.core.graphics.Material;
import org.chapzlock.core.graphics.Mesh;

public class Terrain implements Component {

    private static final float SIZE = 800;
    private static final int VERTEX_COUNT = 128;

    private float x;
    private float z;
    private Mesh mesh;
    private Material material;

}
