package org.chapzlock.core.component;

import org.chapzlock.core.asset.ResourceManager;
import org.chapzlock.core.geometry.RawMeshData;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Mesh implements Component {
    private final int meshHandle;

    public Mesh(RawMeshData rawMeshData) {
        this.meshHandle = ResourceManager.instance().uploadRawMesh(rawMeshData);
    }

    public Mesh(String filePath) {
        this.meshHandle = ResourceManager.instance().uploadMeshFromFile(filePath);
    }
}
