package org.chapzlock.core.scene;

import org.chapzlock.core.render.EntityShaderProps;
import org.chapzlock.core.render.TerrainShaderProps;
import org.chapzlock.core.render.UiShaderProps;

import lombok.Getter;

@Getter
public enum MaterialType {
    ENTITY(EntityShaderProps.VERTEX_FILE, EntityShaderProps.FRAGMENT_FILE),
    TERRAIN(TerrainShaderProps.VERTEX_FILE, TerrainShaderProps.FRAGMENT_FILE),
    UI(UiShaderProps.VERTEX_FILE, UiShaderProps.FRAGMENT_FILE);

    private final String vertexPath;
    private final String fragmentPath;

    MaterialType(String vertexPath, String fragmentPath) {
        this.vertexPath = vertexPath;
        this.fragmentPath = fragmentPath;
    }
}
