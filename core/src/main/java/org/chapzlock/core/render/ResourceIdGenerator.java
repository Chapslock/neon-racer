package org.chapzlock.core.render;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.experimental.UtilityClass;

/**
 * Thread safe id generator for GPU resources.
 */
@UtilityClass
public class ResourceIdGenerator {
    private static final AtomicInteger sequence = new AtomicInteger(0);

    /**
     * Returns the next unique GPU resource ID.
     *
     * <p>Thread-safe. Used by MeshRegistry, TextureRegistry, and ShaderRegistry to assign
     * internal tracking IDs to uploaded GPU resources. These are engine-side IDs, separate
     * from the OpenGL-assigned IDs (which are opaque integers chosen by the GPU driver).
     */
    public static int nextId() {
        return sequence.incrementAndGet();
    }
}
