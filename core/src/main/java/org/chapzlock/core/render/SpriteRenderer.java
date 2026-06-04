package org.chapzlock.core.render;

import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.SpriteMaterial;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.window.WindowSpecification;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Renders 2D sprites with orthographic projection, alpha blending, and no depth testing.
 *
 * <p><strong>What this does:</strong> Takes entities that have a {@link SpriteMaterial}
 * and renders them as flat textured quads in screen-space coordinates. This is used for
 * HUD elements (health bars, score displays), UI overlays, and any 2D content that
 * should appear on top of the 3D world.
 *
 * <p><strong>How it works:</strong>
 * <ol>
 *   <li>Owns and manages the sprite shader program (compiled once at init)</li>
 *   <li>Sets up orthographic projection (pixel coordinates, top-left origin)</li>
 *   <li>Disables depth testing so sprites always render on top</li>
 *   <li>Enables alpha blending for transparency</li>
 *   <li>For each sprite batch (grouped by material): binds texture, sets tint,
 *       then renders each entity's quad at its Transform position</li>
 * </ol>
 *
 * <p><strong>Orthographic projection:</strong> Unlike perspective projection (where
 * distant objects appear smaller), orthographic projection renders everything at its
 * true size regardless of distance. This is essential for UI — a health bar should
 * always be the same size on screen regardless of camera position.
 *
 * @see SpriteMaterial
 * @see SpritePass
 */
public class SpriteRenderer {

    private final ShaderSystem shaderSystem;
    private final TextureRegistry textureRegistry;
    private final Matrix4f projection;
    private Shader shader;

    /**
     * Creates a sprite renderer for the given window dimensions.
     *
     * @param shaderSystem the shader system for compiling and setting uniforms
     * @param textureRegistry the texture registry for binding textures to units
     * @param windowSpec window dimensions used to set up orthographic projection
     */
    public SpriteRenderer(ShaderSystem shaderSystem, TextureRegistry textureRegistry,
                          WindowSpecification windowSpec) {
        this.shaderSystem = shaderSystem;
        this.textureRegistry = textureRegistry;
        this.projection = new Matrix4f().identity()
            .ortho2D(0f, windowSpec.getWidth(), windowSpec.getHeight(), 0f);
    }

    /**
     * Compiles the sprite shader. Call once after the OpenGL context is ready.
     */
    public void init() {
        this.shader = new Shader(SpriteShaderProps.VERTEX_FILE, SpriteShaderProps.FRAGMENT_FILE);
        shaderSystem.compileAndLink(shader);
    }

    /**
     * Begins rendering a batch of sprites sharing the same material.
     * Sets up GL state, binds shader, projection, texture, and tint.
     *
     * @param material the sprite material for this batch
     */
    public void beginBatch(SpriteMaterial material) {
        shaderSystem.use(shader);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        shaderSystem.setUniform(shader, SpriteShaderProps.UNIFORM_PROJECTION_MATRIX, projection);
        textureRegistry.bind(material.texture(), 0);
        shaderSystem.setUniform(shader, SpriteShaderProps.UNIFORM_TEXTURE_SAMPLER, 0);
        shaderSystem.setUniform(shader, SpriteShaderProps.UNIFORM_TINT_COLOR, material.tint());
    }

    /**
     * Sets per-entity transformation matrix uniform.
     *
     * @param transform the entity's screen-space transform
     */
    public void renderEntity(Transform transform) {
        shaderSystem.setUniform(shader,
            SpriteShaderProps.UNIFORM_TRANSFORMATION_MATRIX, transform.getLocalMatrix());
    }

    /**
     * Ends the current batch, restoring GL state.
     */
    public void endBatch() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        shaderSystem.clearProgram();
    }
}
