package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GPU resource handle for a texture (OpenGL texture ID).
 *
 * <p>Created by {@link org.chapzlock.core.render.TextureSystem#load(String)}.
 * Reference-counted — retained on scene load, released on scene unload.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Texture implements Component {
    private int id;
    private int textureHandle;
    private int width;
    private int height;
    private String filePath;
}
