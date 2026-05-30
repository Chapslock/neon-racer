package org.chapzlock.core.render;

import static org.chapzlock.core.render.TerrainShaderProps.*;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.EntityView;

/**
 * Renders terrain with multi-texture blending.
 */
public class TerrainMaterialRenderer implements MaterialRenderer {

    private final ShaderSystem shaderSystem;
    private final TextureSystem textureSystem;

    public TerrainMaterialRenderer(ShaderSystem shaderSystem, TextureSystem textureSystem) {
        this.shaderSystem = shaderSystem;
        this.textureSystem = textureSystem;
    }

    @Override
    public void apply(RenderContext ctx, Material material) {
        Shader shader = material.getShader();
        shaderSystem.use(shader);

        bindTextures(material, shader);
        bindBlendMap(material, shader);

        if (ctx.getSky() != null) {
            shaderSystem.setUniform(shader, UNIFORM_SKY_COLOR, ctx.getSky().getColor().toVector3f());
        }
        shaderSystem.setUniform(shader, UNIFORM_FOG_DENSITY, ctx.getFogDensity());
        shaderSystem.setUniform(shader, UNIFORM_FOG_GRADIENT, ctx.getFogGradient());

        shaderSystem.setUniform(shader, UNIFORM_SHINE_DAMPER, material.getReflection().getShineDamper());
        shaderSystem.setUniform(shader, UNIFORM_REFLECTIVITY, material.getReflection().getReflectivity());
        shaderSystem.setUniform(shader, UNIFORM_VIEW_MATRIX, ctx.getViewMatrix());
        shaderSystem.setUniform(shader, UNIFORM_PROJECTION_MATRIX, ctx.getProjectionMatrix());

        if (ctx.getPointLight() != null) {
            shaderSystem.setUniform(shader, UNIFORM_LIGHT_POSITION, ctx.getPointLight().position());
            shaderSystem.setUniform(shader, UNIFORM_LIGHT_COLOR, ctx.getPointLight().color().toVector3f());
        }
    }

    private void bindTextures(Material material, Shader shader) {
        for (int i = 0; i < material.getTextures().size(); i++) {
            textureSystem.bind(material.getTextures().get(i), i);
        }
        shaderSystem.setUniform(shader, UNIFORM_TEXTURE_0, 0);
        shaderSystem.setUniform(shader, UNIFORM_TEXTURE_1, 1);
        shaderSystem.setUniform(shader, UNIFORM_TEXTURE_2, 2);
    }

    private void bindBlendMap(Material material, Shader shader) {
        Texture blendMap = material.getBlendMap();
        if (blendMap != null) {
            textureSystem.bind(blendMap, material.getTextures().size());
            shaderSystem.setUniform(shader, UNIFORM_TEXTURE_BLEND_MAP, material.getTextures().size());
        }
    }

    @Override
    public void prepareEntity(RenderContext ctx, EntityView entity) {
        shaderSystem.setUniform(entity.get(Material.class).getShader(),
            UNIFORM_TRANSFORMATION_MATRIX, entity.get(Transform.class).getTransformationMatrix());
    }

    @Override
    public void unapply() {
        shaderSystem.clearProgram();
    }
}
