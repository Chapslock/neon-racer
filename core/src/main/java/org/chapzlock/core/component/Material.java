package org.chapzlock.core.component;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.application.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * A generic material component
 */
@Getter
@Builder
@AllArgsConstructor
public final class Material implements Component {
    private Shader shader;
    @Builder.Default
    private List<Texture> textures = new ArrayList<>();
    @Builder.Default
    private Texture blendMap = null;
    @Builder.Default
    private Reflection reflection = Reflection.builder()
        .reflectivity(.1f)
        .shineDamper(1f)
        .build();

    public Material(Shader shader, Texture texture) {
        this(shader, texture, null);
    }

    public Material(Shader shader, Texture texture, Reflection reflection) {
        this.shader = shader;
        this.textures = new ArrayList<>();
        this.textures.add(texture);
        this.reflection = reflection != null ? reflection : Reflection.builder()
            .reflectivity(.1f)
            .shineDamper(1f)
            .build();
    }

    public Texture getFirstTexture() {
        return textures.getFirst();
    }
}
