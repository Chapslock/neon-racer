package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Internal representation of an OpenGL texture.
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
