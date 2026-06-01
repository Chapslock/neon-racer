package org.chapzlock.core.component;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector4f;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Material component pairing a shader with textures and rendering properties.
 *
 * <p>Each material type (PBR, UI) has a corresponding {@link org.chapzlock.core.render.MaterialRenderer}
 * that knows how to bind uniforms and textures.
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
    private Vector4f tint = new Vector4f(1f, 1f, 1f, 1f);

    public Material(Shader shader, Texture texture) {
        this.shader = shader;
        this.textures = new ArrayList<>();
        this.textures.add(texture);
    }

    public Texture getFirstTexture() {
        return textures.getFirst();
    }
}
