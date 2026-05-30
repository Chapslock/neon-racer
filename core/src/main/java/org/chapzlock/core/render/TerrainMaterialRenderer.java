package org.chapzlock.core.render;

import static org.chapzlock.core.render.TerrainShaderProps.*;

import java.util.List;

import org.chapzlock.core.component.*;
import org.chapzlock.core.ecs.EntityView;
import org.joml.Vector3f;

/**
 * Renders terrain with multi-texture blending and multi-light Phong lighting.
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

        shaderSystem.setUniform(shader, UNIFORM_VIEW_MATRIX, ctx.getViewMatrix());
        shaderSystem.setUniform(shader, UNIFORM_PROJECTION_MATRIX, ctx.getProjectionMatrix());
        shaderSystem.setUniform(shader, UNIFORM_CAMERA_POSITION, ctx.getCameraPosition());

        shaderSystem.setUniform(shader, UNIFORM_SHINE_DAMPER, material.getReflection().getShineDamper());
        shaderSystem.setUniform(shader, UNIFORM_REFLECTIVITY, material.getReflection().getReflectivity());

        boolean hasDirLight = ctx.getDirectionalLight() != null;
        shaderSystem.setUniform(shader, UNIFORM_HAS_DIR_LIGHT, hasDirLight);
        if (hasDirLight) {
            shaderSystem.setUniform(shader, UNIFORM_DIR_LIGHT_DIRECTION,
                ctx.getDirectionalLight().getDirection());
            shaderSystem.setUniform(shader, UNIFORM_DIR_LIGHT_COLOR,
                ctx.getDirectionalLight().getColor().toVector3f());
        }

        List<PointLight> lights = ctx.getPointLights();
        shaderSystem.setUniform(shader, UNIFORM_NUM_POINT_LIGHTS, lights.size());
        for (int i = 0; i < lights.size(); i++) {
            PointLight pl = lights.get(i);
            shaderSystem.setUniform(shader, UNIFORM_POINT_LIGHT_POSITIONS + "[" + i + "]",
                pl.getPosition());
            shaderSystem.setUniform(shader, UNIFORM_POINT_LIGHT_COLORS + "[" + i + "]",
                pl.getColor().toVector3f());
            Attenuation att = pl.getAttenuation();
            shaderSystem.setUniform(shader, UNIFORM_POINT_LIGHT_ATTENUATION + "[" + i + "]",
                new Vector3f(att.constant(), att.linear(), att.quadratic()));
        }

        shaderSystem.setUniform(shader, UNIFORM_AMBIENT_STRENGTH, ctx.getEffectiveAmbientStrength());

        if (ctx.getSky() != null) {
            shaderSystem.setUniform(shader, UNIFORM_SKY_COLOR, ctx.getSky().getColor().toVector3f());
        }
        shaderSystem.setUniform(shader, UNIFORM_FOG_ENABLED, ctx.isFogEnabled());
        if (ctx.isFogEnabled()) {
            shaderSystem.setUniform(shader, UNIFORM_FOG_DENSITY, ctx.getFog().getDensity());
            shaderSystem.setUniform(shader, UNIFORM_FOG_GRADIENT, ctx.getFog().getGradient());
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
