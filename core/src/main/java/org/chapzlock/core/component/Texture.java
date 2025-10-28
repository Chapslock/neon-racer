package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;

/**
 * Component for an OpenGL texture.
 */
public class Texture implements Component {
    private final int handle;
    private final int width;
    private final int height;
    private final String filePath;

    public Texture(int handle, int width, int height, String filePath) {
        this.handle = handle;
        this.width = width;
        this.height = height;
        this.filePath = filePath;
    }

    public int getHandle() {
        return handle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getFilePath() {
        return filePath;
    }
}
