package org.chapzlock.core.graphics.material;

import org.chapzlock.core.application.Application;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.graphics.shader.UiShaderProps;
import org.chapzlock.core.system.CameraSystem;
import org.chapzlock.core.system.ShaderSystem;
import org.chapzlock.core.system.TextureSystem;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * GUI renderer: uses orthographic projection (screen pixels),
 * disables depth testing, enables alpha blending and draws textured quads.
 * <p>
 * Assumes the Mesh for GUI is a unit quad at origin with positions in [0..1] or [-0.5..0.5]
 * (you can adapt RawMeshDataFactory to create a GUI quad).
 */
public class UiMaterialRenderer implements MaterialRenderer {

    private final ShaderSystem shaderSystem = ShaderSystem.instance();
    private final TextureSystem textureSystem = TextureSystem.instance();
    private final CameraSystem cameraSystem = new CameraSystem();
    private final Matrix4f projecton = new Matrix4f() //TODO: Refactor to use events
        .identity()
        .ortho2D(0f,
            Application.instance().getAppSpec().getWindowSpec().getWidth(),
            Application.instance().getAppSpec().getWindowSpec().getHeight(),
            0f
        );

    @Override
    public void apply(Material material, Camera camera, PointLight light, Sky sky) {
        Shader shader = material.getShader();
        shaderSystem.use(shader);

        // UI GL state
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        shaderSystem.setUniform(shader, UiShaderProps.UNIFORM_PROJECTION_MATRIX, projecton);

        Texture texture = material.getFirstTexture();
        if (texture != null) {
            textureSystem.bind(texture, 0);
            shaderSystem.setUniform(shader, UiShaderProps.UNIFORM_TEXTURE_SAMPLER, 0);
        }

        // If material holds tint (GuiMaterial), upload it as well (per-batch fallback); per-entity tint could override
        shaderSystem.setUniform(shader, UiShaderProps.UNIFORM_TINT_COLOR, material.getTint());
    }

    @Override
    public void prepareEntity(EntityView entity, Material material) {
        Transform transform = entity.get(Transform.class);
        shaderSystem.setUniform(material.getShader(), UiShaderProps.UNIFORM_TRANSFORMATION_MATRIX, transform.getTransformationMatrix());
    }

    @Override
    public void unapply(Material material) {
        Texture tex = material.getFirstTexture();
        if (tex != null) {
            textureSystem.unbind(tex);
        }

        // Restore GL state
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);

        shaderSystem.clearProgram();
    }

}
