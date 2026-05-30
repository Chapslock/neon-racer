package org.chapzlock.core.render;

import java.util.Map;

import org.chapzlock.core.application.Disposable;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.asset.FileUtils;
import org.chapzlock.core.asset.RawMeshData;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Loads, caches, and manages GPU mesh resources.
 *
 * <p>Meshes are reference-counted — use {@link #retain(org.chapzlock.core.component.Mesh)} and
 * {@link #release(org.chapzlock.core.component.Mesh)} to manage lifetimes across scenes.
 */
public class MeshSystem implements Disposable {

    private final Map<Integer, Mesh> meshPool = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, RawMeshData> rawMeshDataPool = new Int2ObjectOpenHashMap<>();
    private final Map<String, Integer> resourcePathToIdMap = new Object2IntOpenHashMap<>();
    private final Map<RawMeshData, Integer> rawMeshDataToIdMap = new Object2IntOpenHashMap<>();
    private final Int2IntOpenHashMap refCounts = new Int2IntOpenHashMap();

    public MeshSystem() {
    }

    /**
     * Loads mesh data from an .obj file and uploads it to GPU
     *
     * @param resourcePath file path to the .obj file
     * @return uploaded mesh component
     */
    public Mesh load(String resourcePath) {
        Integer existingId = resourcePathToIdMap.get(resourcePath);
        if (existingId != null) {
            return meshPool.get(existingId);
        }
        RawMeshData rawMeshData = FileUtils.loadMeshData(resourcePath);
        Mesh mesh = MeshUtil.bindMeshDataToGpu(rawMeshData);
        meshPool.put(mesh.getId(), mesh);
        rawMeshDataPool.put(mesh.getId(), rawMeshData);
        rawMeshDataToIdMap.put(rawMeshData, mesh.getId());
        resourcePathToIdMap.put(resourcePath, mesh.getId());
        return mesh;
    }

    /**
     * uploads raw mesh data to the GPU
     *
     * @param rawMeshData
     * @return uploaded mesh component
     */
    public Mesh load(RawMeshData rawMeshData) {
        Integer existingId = rawMeshDataToIdMap.get(rawMeshData);
        if (existingId != null) {
            return meshPool.get(existingId);
        }
        Mesh mesh = MeshUtil.bindMeshDataToGpu(rawMeshData);
        meshPool.put(mesh.getId(), mesh);
        rawMeshDataPool.put(mesh.getId(), rawMeshData);
        rawMeshDataToIdMap.put(rawMeshData, mesh.getId());
        return mesh;
    }

    /**
     * Renders a mesh
     */
    public void render(Mesh mesh) {
        MeshUtil.render(mesh);
    }

    /**
     * Finds the Raw mesh data associated with the mesh component
     *
     * @param id of the mesh component
     * @return RawMeshData for the given mesh
     */
    public RawMeshData getRawMeshById(int id) {
        return rawMeshDataPool.get(id);
    }

    public void retain(int assetId) {
        refCounts.put(assetId, refCounts.getOrDefault(assetId, 0) + 1);
    }

    public void release(int assetId) {
        int count = refCounts.getOrDefault(assetId, 0) - 1;
        if (count <= 0) {
            refCounts.remove(assetId);
            Mesh mesh = meshPool.remove(assetId);
            if (mesh != null) {
                MeshUtil.delete(mesh);
            }
            rawMeshDataPool.remove(assetId);
            resourcePathToIdMap.values().removeIf(id -> id.equals(assetId));
            rawMeshDataToIdMap.values().removeIf(id -> id.equals(assetId));
        } else {
            refCounts.put(assetId, count);
        }
    }

    public int getRefCount(int assetId) {
        return refCounts.getOrDefault(assetId, 0);
    }

    @Override
    public void dispose() {
        for (Mesh mesh : meshPool.values()) {
            MeshUtil.delete(mesh);
        }
        meshPool.clear();
        rawMeshDataPool.clear();
        resourcePathToIdMap.clear();
        rawMeshDataToIdMap.clear();
    }
}
