package org.chapzlock.core.system;

import java.util.Map;

import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.files.FileUtils;
import org.chapzlock.core.geometry.RawMeshData;
import org.chapzlock.core.graphics.MeshUtil;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Caches and handles OpenGL calls on Meshes.
 * Implemented as a singleton to optimize GPU resource usage
 */
public class MeshSystem {
    private static MeshSystem instance;

    private final Map<Integer, Mesh> meshPool = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, RawMeshData> rawMeshDataPool = new Int2ObjectOpenHashMap<>();
    private final Map<String, Integer> resourcePathToIdMap = new Object2IntOpenHashMap<>();
    private final Map<RawMeshData, Integer> rawMeshDataToIdMap = new Object2IntOpenHashMap<>();

    private MeshSystem() {
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

    public static MeshSystem instance() {
        if (instance == null) {
            instance = new MeshSystem();
        }

        return instance;
    }

}
