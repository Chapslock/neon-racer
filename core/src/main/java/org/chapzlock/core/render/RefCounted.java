package org.chapzlock.core.render;

import java.util.function.IntConsumer;

import org.chapzlock.core.logging.Log;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * Tracks reference counts for integer resource IDs and fires a callback when count reaches zero.
 *
 * <p><strong>What is reference counting?</strong> A resource management technique where each
 * "user" of a resource increments a counter when they start using it and decrements when they
 * stop. When the counter reaches zero, nobody is using the resource anymore and it can be freed.
 *
 * <p><strong>Why this exists:</strong> GPU resources (meshes, textures, shaders) are expensive
 * to upload and should be shared across scenes. When scene A and scene B both use the same
 * texture, we upload it once and keep a count of 2. When scene A unloads, the count drops to 1
 * (still in use by B). Only when scene B also unloads does the count hit 0, triggering deletion
 * from GPU memory.
 *
 * <p><strong>Usage:</strong> Each GPU resource system ({@link MeshRegistry}, {@link TextureRegistry},
 * {@link ShaderRegistry}) owns a RefCounted instance with a cleanup callback that frees the
 * specific resource type.
 *
 * @see MeshRegistry
 * @see TextureRegistry
 * @see ShaderRegistry
 */
public class RefCounted {

    private final Int2IntOpenHashMap refCounts = new Int2IntOpenHashMap();
    private final IntConsumer onRelease;

    /**
     * Creates a RefCounted tracker with the given cleanup callback.
     *
     * @param onRelease called with the resource ID when its count drops to zero
     */
    public RefCounted(IntConsumer onRelease) {
        this.onRelease = onRelease;
    }

    /**
     * Increments the reference count for a resource.
     *
     * @param id the resource ID to retain
     */
    public void retain(int id) {
        refCounts.put(id, refCounts.getOrDefault(id, 0) + 1);
    }

    /**
     * Decrements the reference count for a resource. If it reaches zero, the
     * {@code onRelease} callback is fired and tracking for this ID is removed.
     *
     * @param id the resource ID to release
     */
    public void release(int id) {
        int current = refCounts.getOrDefault(id, 0);
        if (current <= 0) {
            Log.warn("RefCounted", "release() called on id=" + id + " with refCount=" + current + ". Ignoring.");
            return;
        }
        int count = current - 1;
        if (count == 0) {
            refCounts.remove(id);
            onRelease.accept(id);
        } else {
            refCounts.put(id, count);
        }
    }

    /**
     * Returns the current reference count for a resource.
     *
     * @param id the resource ID to query
     * @return the number of active references, or 0 if not tracked
     */
    public int getRefCount(int id) {
        return refCounts.getOrDefault(id, 0);
    }
}
