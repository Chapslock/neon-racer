package org.chapzlock.core.render;

import java.util.Map;

import org.chapzlock.core.application.Disposable;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.asset.RawMeshData;
import org.chapzlock.core.logging.Log;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Uploads, caches, and manages GPU mesh (geometry) resources.
 *
 * <p><strong>What is a mesh?</strong> A mesh is 3D geometry — the shape of an object defined
 * by vertices (corner points), normals (surface directions for lighting), UV coordinates
 * (for texturing), and indices (which vertices form triangles). A mesh lives on the GPU in
 * Vertex Buffer Objects (VBOs) grouped into a Vertex Array Object (VAO).
 *
 * <p><strong>What this registry does:</strong>
 * <ul>
 *   <li><em>Upload:</em> Takes CPU-side {@link RawMeshData} (float/int arrays) and uploads
 *       it to the GPU via {@link MeshUtil#bindMeshDataToGpu}. Returns a {@link Mesh}
 *       component holding the GPU resource IDs.</li>
 *   <li><em>Deduplicate:</em> If the same {@link RawMeshData} is uploaded again (same object
 *       instance), returns the existing GPU mesh instead of re-uploading. This happens when
 *       multiple entities share the same geometry (e.g., multiple instances of a tree model).</li>
 *   <li><em>Render:</em> Binds a mesh's VAO and issues the draw call.</li>
 *   <li><em>Lifecycle:</em> Reference counting ensures meshes stay on the GPU as long as at
 *       least one scene uses them, and are freed when the last scene unloads.</li>
 * </ul>
 *
 * <p><strong>Reference counting:</strong> When a scene is loaded, SceneManager calls
 * {@link #retain(int)} for each mesh used by that scene. When the scene is unloaded,
 * {@link #release(int)} is called. The mesh is deleted from GPU memory only when its
 * reference count drops to zero.
 *
 * <p><strong>Usage:</strong> Game code does not interact with this registry directly. The
 * {@link org.chapzlock.core.scene.GltfModelFactory} and
 * {@link org.chapzlock.core.ecs.EntitySystem} use it during scene instantiation.
 *
 * @see Mesh
 * @see MeshUtil
 * @see RawMeshData
 */
public class MeshRegistry implements Disposable {

    private static final Log log = Log.create(MeshRegistry.class);

    /** All uploaded meshes, indexed by their engine-assigned ID. */
    private final Map<Integer, Mesh> meshPool = new Int2ObjectOpenHashMap<>();

    /** Maps mesh ID → the original CPU-side data (needed for physics shape generation). */
    private final Map<Integer, RawMeshData> rawMeshDataPool = new Int2ObjectOpenHashMap<>();

    /** Deduplication map: if the same RawMeshData instance is uploaded again, reuse existing. */
    private final Map<RawMeshData, Integer> rawMeshDataToIdMap = new Object2IntOpenHashMap<>();

    /** Reverse lookup: mesh ID → original RawMeshData. Enables O(1) cleanup on release. */
    private final Map<Integer, RawMeshData> idToRawMeshDataMap = new Int2ObjectOpenHashMap<>();

    /**
     * Reference counting tracker for mesh lifecycle.
     *
     * <p>When the reference count for a mesh ID drops to zero (no scenes use it anymore),
     * the {@link #deleteMesh(int)} callback is fired to clean up GPU resources.
     */
    private final RefCounted refCounted = new RefCounted(this::deleteMesh);

    public MeshRegistry() {
    }

    /**
     * Uploads mesh data to the GPU, or returns an existing mesh if this data was already uploaded.
     *
     * <p>If the same {@link RawMeshData} instance was previously uploaded, the cached
     * {@link Mesh} is returned without re-uploading. This deduplication works by object
     * identity — two separate RawMeshData objects with identical content will be uploaded
     * separately (which is correct, as they may represent different instances).
     *
     * @param rawMeshData the CPU-side vertex/index data to upload
     * @return a Mesh component holding the GPU resource IDs (VAO, VBOs)
     */
    public Mesh load(RawMeshData rawMeshData) {
        Integer existingId = rawMeshDataToIdMap.get(rawMeshData);
        if (existingId != null) {
            log.debug("Mesh cache hit: id=%d (%d indices)", existingId, meshPool.get(existingId).getVertexCount());
            return meshPool.get(existingId);
        }
        Mesh mesh = MeshUtil.bindMeshDataToGpu(rawMeshData);
        meshPool.put(mesh.getId(), mesh);
        rawMeshDataPool.put(mesh.getId(), rawMeshData);
        rawMeshDataToIdMap.put(rawMeshData, mesh.getId());
        idToRawMeshDataMap.put(mesh.getId(), rawMeshData);
        log.debug("Mesh uploaded to GPU: id=%d, %d indices", mesh.getId(), mesh.getVertexCount());
        return mesh;
    }

    /**
     * Binds the mesh's VAO and issues a draw call for all its triangles.
     *
     * <p>The mesh must have been previously uploaded via {@link #load(RawMeshData)}.
     * The appropriate shader program should be active before calling this.
     *
     * @param mesh the mesh to render
     */
    public void render(Mesh mesh) {
        MeshUtil.render(mesh);
    }

    /**
     * Returns the original CPU-side mesh data for a given mesh ID.
     *
     * <p>Used by the physics system to generate collision shapes from mesh geometry
     * (e.g., convex hull or triangle mesh shapes).
     *
     * @param id the engine-assigned mesh ID (from {@link Mesh#getId()})
     * @return the original RawMeshData, or null if not found
     */
    public RawMeshData getRawMeshById(int id) {
        return rawMeshDataPool.get(id);
    }

    /**
     * Increments the reference count for a mesh.
     *
     * <p>Called by SceneManager when a scene using this mesh is loaded.
     *
     * @param assetId the engine-assigned mesh ID
     */
    public void retain(int assetId) {
        refCounted.retain(assetId);
    }

    /**
     * Decrements the reference count for a mesh.
     *
     * <p>If the count reaches zero, the mesh is deleted from the GPU. Called by
     * SceneManager when a scene using this mesh is unloaded.
     *
     * @param assetId the engine-assigned mesh ID
     */
    public void release(int assetId) {
        refCounted.release(assetId);
    }

    /**
     * Returns the current reference count for a mesh.
     *
     * @param assetId the engine-assigned mesh ID
     * @return the number of scenes currently using this mesh
     */
    public int getRefCount(int assetId) {
        return refCounted.getRefCount(assetId);
    }

    /**
     * Deletes a mesh from GPU memory and clears all associated caches.
     *
     * <p>Called by the {@link RefCounted} tracker when a mesh's reference count drops to zero.
     * Removes the mesh from all internal maps and frees GPU resources via {@link MeshUtil#delete}.
     *
     * @param assetId the engine-assigned mesh ID to delete
     */
    private void deleteMesh(int assetId) {
        Mesh mesh = meshPool.remove(assetId);
        if (mesh != null) {
            MeshUtil.delete(mesh);
            log.debug("Mesh released and deleted from GPU: id=%d", assetId);
        }
        rawMeshDataPool.remove(assetId);
        RawMeshData rawData = idToRawMeshDataMap.remove(assetId);
        if (rawData != null) {
            rawMeshDataToIdMap.remove(rawData);
        }
    }

    /**
     * Frees ALL meshes from GPU memory. Called during engine shutdown.
     *
     * <p>Reference counts are ignored — everything is cleaned up unconditionally.
     */
    @Override
    public void dispose() {
        for (Mesh mesh : meshPool.values()) {
            MeshUtil.delete(mesh);
        }
        meshPool.clear();
        rawMeshDataPool.clear();
        rawMeshDataToIdMap.clear();
        idToRawMeshDataMap.clear();
    }
}
