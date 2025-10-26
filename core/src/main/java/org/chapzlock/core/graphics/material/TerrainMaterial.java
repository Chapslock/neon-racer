package org.chapzlock.core.graphics.material;

import static org.lwjgl.opengl.GL20.GL_TEXTURE0;
import static org.lwjgl.opengl.GL20.glActiveTexture;

import org.chapzlock.core.component.Component;
import org.chapzlock.core.graphics.Reflection;
import org.chapzlock.core.graphics.Texture;
import org.chapzlock.core.graphics.shader.TerrainStaticShader;

import lombok.Getter;

/**
 * A generic Terrain material
 */
public class TerrainMaterial implements Component {

    @Getter
    private final TerrainStaticShader shader;
    private final Texture texture; // nullable

    @Getter
    private final Reflection reflection;

    public TerrainMaterial(Texture texture) {
        this(texture, null);
    }

    public TerrainMaterial(Texture texture, Reflection reflection) {
        this.shader = new TerrainStaticShader();
        this.texture = texture;
        this.reflection = reflection != null ? reflection : Reflection.builder()
            .reflectivity(.1f)
            .shineDamper(1f)
            .build();
    }

    public void bind() {
        shader.bind();

        if (texture != null) {
            glActiveTexture(GL_TEXTURE0);
            texture.bind();
        }

        shader.loadShine(reflection.getShineDamper(), reflection.getReflectivity());
    }

    public void unbind() {
        if (texture != null) {
            texture.unbind();
        }
        shader.unbind();
    }
}
