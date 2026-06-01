package org.chapzlock.core.render;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.window.WindowSpecification;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Renders UI sprites with orthographic projection.
 *
 * <p>Disables depth testing, enables alpha blending, draws textured quads
 * in screen-space coordinates.</p>
 */
public class UiMaterialRenderer implements MaterialRenderer {

    private final ShaderSystem shaderSystem;
    private final TextureSystem textureSystem;
    private final Matrix4f projection;

    public UiMaterialRenderer(ShaderSystem shaderSystem, TextureSystem textureSystem, WindowSpecification windowSpec) {
        this.shaderSystem = shaderSystem;
        this.textureSystem = textureSystem;
        this.projection = new Matrix4f()
            .identity()
            .ortho2D(0f, windowSpec.getWidth(), windowSpec.getHeight(), 0f);
    }

    @Override
    public void apply(RenderContext ctx, Material material) {
        Shader shader = material.getShader();
        shaderSystem.use(shader);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        shaderSystem.setUniform(shader, UiShaderProps.UNIFORM_PROJECTION_MATRIX, projection);

        Texture texture = material.getFirstTexture();
        if (texture != null) {
            textureSystem.bind(texture, 0);
            shaderSystem.setUniform(shader, UiShaderProps.UNIFORM_TEXTURE_SAMPLER, 0);
        }

        shaderSystem.setUniform(shader, UiShaderProps.UNIFORM_TINT_COLOR, material.getTint());
    }

    @Override
    public void prepareEntity(RenderContext ctx, int entityId) {
        Transform transform = ctx.getRegistry().getComponent(entityId, Transform.class);
        Material material = ctx.getRegistry().getComponent(entityId, Material.class);
        shaderSystem.setUniform(material.getShader(),
            UiShaderProps.UNIFORM_TRANSFORMATION_MATRIX, transform.getTransformationMatrix());
    }

    @Override
    public void unapply() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        shaderSystem.clearProgram();
    }
}
