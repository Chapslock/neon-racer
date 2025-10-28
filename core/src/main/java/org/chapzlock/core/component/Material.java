package org.chapzlock.core.component;

import org.chapzlock.core.application.Component;

/**
 * Material component for terrain.
 *
 */
public final class Material implements Component {
    private final Shader shader;
    private final Texture texture;
    private final Reflection reflection;

    public Material(Shader shader, Texture texture) {
        this(shader, texture, null);
    }

    public Material(Shader shader, Texture texture, Reflection reflection) {
        this.shader = shader;
        this.texture = texture;
        this.reflection = reflection != null ? reflection : Reflection.builder()
            .reflectivity(.1f)
            .shineDamper(1f)
            .build();
    }

    public Shader getShader() {
        return shader;
    }

    public Texture getTexture() {
        return texture;
    }

    public Reflection getReflection() {
        return reflection;
    }
}
