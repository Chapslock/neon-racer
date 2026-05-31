package org.chapzlock.core.scene;

import org.chapzlock.core.render.EntityShaderProps;
import org.chapzlock.core.render.PbrShaderProps;
import org.chapzlock.core.render.UiShaderProps;

import lombok.Getter;

/**
 * Built-in material types with associated shaders.
 *
 * <p>Each type maps to a default {@link org.chapzlock.core.render.MaterialRenderer}:
 * <ul>
 *   <li>{@link #ENTITY} — standard 3D models with lighting</li>
 *   <li>{@link #PBR} — PBR metallic-roughness rendering for glTF assets</li>
 *   <li>{@link #UI} — 2D UI elements with orthographic projection</li>
 * </ul>
 */
@Getter
public enum MaterialType {
    /** Standard 3D entity rendering with lighting. */
    ENTITY(EntityShaderProps.VERTEX_FILE, EntityShaderProps.FRAGMENT_FILE),
    /** PBR metallic-roughness rendering. The primary material type for glTF assets. */
    PBR(PbrShaderProps.VERTEX_FILE, PbrShaderProps.FRAGMENT_FILE),
    /** 2D UI rendering with orthographic projection. */
    UI(UiShaderProps.VERTEX_FILE, UiShaderProps.FRAGMENT_FILE);

    private final String vertexPath;
    private final String fragmentPath;

    MaterialType(String vertexPath, String fragmentPath) {
        this.vertexPath = vertexPath;
        this.fragmentPath = fragmentPath;
    }
}
