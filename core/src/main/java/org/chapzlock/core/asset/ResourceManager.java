package org.chapzlock.core.asset;

import java.util.Map;

import org.chapzlock.core.files.FileUtils;
import org.chapzlock.core.geometry.RawMeshData;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Handles operations performed on resources allocated on the GPU
 */
public class ResourceManager {
    private static ResourceManager instance;

    private final Map<Integer, RawMesh> meshPool = new Int2ObjectOpenHashMap<>();
    private final Map<String, Integer> resourcePathToIdMap = new Object2IntOpenHashMap<>();
    private final Map<RawMeshData, Integer> rawMeshToIdMap = new Object2IntOpenHashMap<>();

    private ResourceManager() {
    }

    /**
     * Loads mesh data from an .obj file and uploads it to GPU
     *
     * @param resourcePath file path to the .obj file
     * @return internal handle for the Mesh rsource
     */
    public int uploadMeshFromFile(String resourcePath) {
        Integer existingHandle = resourcePathToIdMap.get(resourcePath);
        if (existingHandle != null) {
            return existingHandle;
        }
        RawMeshData rawMeshData = FileUtils.loadWavefrontFileToMesh(resourcePath);
        RawMesh rawMesh = RawMeshManager.bindMeshDataToGpu(rawMeshData);
        int handle = ResourceIdGenerator.nextId();
        meshPool.put(handle, rawMesh);
        resourcePathToIdMap.put(resourcePath, handle);
        rawMeshToIdMap.put(rawMeshData, handle);
        return handle;
    }

    /**
     * uploads raw mesh data to the GPU
     *
     * @param rawMeshData
     * @return internal handle for the mesh resource
     */
    public int uploadRawMesh(RawMeshData rawMeshData) {
        Integer existingHandle = rawMeshToIdMap.get(rawMeshData);
        if (existingHandle != null) {
            return existingHandle;
        }
        RawMesh rawMesh = RawMeshManager.bindMeshDataToGpu(rawMeshData);
        int handle = ResourceIdGenerator.nextId();
        meshPool.put(handle, rawMesh);
        rawMeshToIdMap.put(rawMeshData, handle);
        return handle;
    }

    /**
     * Renders a mesh based on its resource handle
     *
     * @param handle
     */
    public void renderMesh(int handle) {
        RawMesh mesh = meshPool.get(handle);
        RawMeshManager.render(mesh);
    }

    public static ResourceManager instance() {
        if (instance == null) {
            instance = new ResourceManager();
        }

        return instance;
    }

}
