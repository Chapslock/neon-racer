package org.chapzlock.core.system;

import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_REFLECTIVITY;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_SHINE_DAMPER;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_TEXTURE_SAMPLER;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Texture;

public class EntityMaterialSystem {
    private final ShaderSystem shaderSystem;
    private final TextureSystem textureSystem = TextureSystem.instance();

    public EntityMaterialSystem(ShaderSystem shaderSystem) {
        this.shaderSystem = shaderSystem;
    }

    /**
     * Apply a terrain material: bind shader + texture and set common uniforms.
     */
    public void applyMaterial(Material material) {
        Shader shader = material.getShader();
        shaderSystem.use(shader);

        Texture tex = material.getTexture();
        if (tex != null) {
            textureSystem.bind(tex, 0);
            shaderSystem.setUniform(shader, UNIFORM_TEXTURE_SAMPLER, 0);
        }

        shaderSystem.setUniform(shader, UNIFORM_SHINE_DAMPER, material.getReflection().getShineDamper());
        shaderSystem.setUniform(shader, UNIFORM_REFLECTIVITY, material.getReflection().getReflectivity());
    }

    public void unapplyMaterial(Material material) {
        Texture tex = material.getTexture();
        if (tex != null) {
            textureSystem.unbind(tex);
        }
        shaderSystem.stop();
    }
}
