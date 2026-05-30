package org.chapzlock.core.render;

import static org.chapzlock.core.render.EntityShaderProps.*;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.EntityView;

/**
 * Renders standard 3D entities with Phong lighting.
 */
public class EntityMaterialRenderer implements MaterialRenderer {

    private final ShaderSystem shaderSystem;
    private final TextureSystem textureSystem;

    public EntityMaterialRenderer(ShaderSystem shaderSystem, TextureSystem textureSystem) {
        this.shaderSystem = shaderSystem;
        this.textureSystem = textureSystem;
    }

    @Override
    public void apply(RenderContext ctx, Material material) {
        Shader shader = material.getShader();
        shaderSystem.use(shader);

        Texture texture = material.getFirstTexture();
        if (texture != null) {
            textureSystem.bind(texture, 0);
            shaderSystem.setUniform(shader, UNIFORM_TEXTURE_SAMPLER, 0);
        }

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
