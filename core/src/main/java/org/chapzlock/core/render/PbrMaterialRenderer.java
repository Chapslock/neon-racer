package org.chapzlock.core.render;

import static org.chapzlock.core.render.PbrShaderProps.*;

import java.util.List;

import org.chapzlock.core.component.*;
import org.chapzlock.core.ecs.EntityView;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Renders entities with PBR metallic-roughness materials.
 *
 * <p>Implements the Cook-Torrance BRDF rendering model. Binds PBR texture maps
 * (base color, metallic-roughness, normal, occlusion, emissive) and sets all
 * uniforms needed by the PBR fragment shader.
 *
 * <p><strong>How PBR rendering works at a high level:</strong>
 * <ol>
 *   <li>Bind the PBR shader program</li>
 *   <li>Set camera/projection matrices (per-frame, shared across all entities)</li>
 *   <li>Set lighting uniforms (directional + point lights)</li>
 *   <li>Set material properties (metallic, roughness, textures)</li>
 *   <li>For each entity: set model matrix, draw mesh</li>
 * </ol>
 *
 * <p><strong>Texture unit layout:</strong>
 * <ul>
 *   <li>Unit 0: base color map</li>
 *   <li>Unit 1: metallic-roughness map</li>
 *   <li>Unit 2: normal map</li>
 *   <li>Unit 3: occlusion map</li>
 *   <li>Unit 4: emissive map</li>
 * </ul>
 *
 * @see PbrMaterial
 * @see PbrShaderProps
 */
public class PbrMaterialRenderer implements MaterialRenderer {

    private final ShaderSystem shaderSystem;
    private final TextureSystem textureSystem;

    /**
     * Reusable temporary matrix for computing joint skin matrices each frame.
     *
     * <p>Allocating a new Matrix4f per joint per entity per frame would create massive GC
     * pressure (128 joints * N entities * 60 fps). Instead we reuse this single instance,
     * overwriting it for each joint during upload.
     */
    private final Matrix4f tempJointMatrix = new Matrix4f();

    public PbrMaterialRenderer(ShaderSystem shaderSystem, TextureSystem textureSystem) {
        this.shaderSystem = shaderSystem;
        this.textureSystem = textureSystem;
    }

    @Override
    public void apply(RenderContext ctx, Material material) {
        // PbrMaterialRenderer works with PbrMaterial, not the legacy Material component.
        // During the transition period, use applyPbr() directly from the render pass.
        throw new UnsupportedOperationException(
            "PbrMaterialRenderer does not support legacy Material. Use applyPbr(RenderContext, PbrMaterial) instead.");
    }

    /**
     * Binds the PBR shader and sets per-batch uniforms (projection, view, lighting, material).
     *
     * <p>Call this once per material batch before rendering entities that share this material.
     * Then call {@link #prepareEntity(RenderContext, EntityView)} for each entity to set
     * its model matrix.
     *
     * @param ctx the current frame's render context (camera, lights)
     * @param material the PBR material to bind
     */
    public void applyPbr(RenderContext ctx, PbrMaterial material) {
        Shader shader = material.getShader();
        shaderSystem.use(shader);

        // Matrices
        shaderSystem.setUniform(shader, UNIFORM_VIEW_MATRIX, ctx.getViewMatrix());
        shaderSystem.setUniform(shader, UNIFORM_PROJECTION_MATRIX, ctx.getProjectionMatrix());

        // Material factors
        shaderSystem.setUniform(shader, UNIFORM_BASE_COLOR_FACTOR, material.getBaseColorFactor());
        shaderSystem.setUniform(shader, UNIFORM_METALLIC_FACTOR, material.getMetallicFactor());
        shaderSystem.setUniform(shader, UNIFORM_ROUGHNESS_FACTOR, material.getRoughnessFactor());
        shaderSystem.setUniform(shader, UNIFORM_EMISSIVE_FACTOR, material.getEmissiveFactor());
        shaderSystem.setUniform(shader, UNIFORM_ALPHA_CUTOFF, material.getAlphaCutoff());
        shaderSystem.setUniform(shader, UNIFORM_ALPHA_MODE, material.getAlphaMode().ordinal());

        // Textures
        bindTextureIfPresent(shader, material.getBaseColorMap(), 0,
            UNIFORM_BASE_COLOR_MAP, UNIFORM_HAS_BASE_COLOR_MAP);
        bindTextureIfPresent(shader, material.getMetallicRoughnessMap(), 1,
            UNIFORM_METALLIC_ROUGHNESS_MAP, UNIFORM_HAS_METALLIC_ROUGHNESS_MAP);
        bindTextureIfPresent(shader, material.getNormalMap(), 2,
            UNIFORM_NORMAL_MAP, UNIFORM_HAS_NORMAL_MAP);
        bindTextureIfPresent(shader, material.getOcclusionMap(), 3,
            UNIFORM_OCCLUSION_MAP, UNIFORM_HAS_OCCLUSION_MAP);
        bindTextureIfPresent(shader, material.getEmissiveMap(), 4,
            UNIFORM_EMISSIVE_MAP, UNIFORM_HAS_EMISSIVE_MAP);

        // Camera position (needed for specular calculations in fragment shader)
        shaderSystem.setUniform(shader, UNIFORM_CAMERA_POSITION, ctx.getCameraPosition());
        shaderSystem.setUniform(shader, UNIFORM_AMBIENT_STRENGTH, ctx.getEffectiveAmbientStrength());

        // Directional light
        boolean hasDirLight = ctx.getDirectionalLight() != null;
        shaderSystem.setUniform(shader, UNIFORM_HAS_DIR_LIGHT, hasDirLight);
        if (hasDirLight) {
            shaderSystem.setUniform(shader, UNIFORM_DIR_LIGHT_DIRECTION,
                ctx.getDirectionalLight().getDirection());
            shaderSystem.setUniform(shader, UNIFORM_DIR_LIGHT_COLOR,
                ctx.getDirectionalLight().getColor().toVector3f());
        }

        // Point lights
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
    }

    /**
     * Sets the per-entity model (world) transformation matrix and skinning uniforms.
     *
     * <p>Prefers {@link WorldTransform} (computed by TransformSystem for hierarchy support).
     * Falls back to {@link Transform#getTransformationMatrix()} for standalone entities.
     *
     * <p><strong>GPU Skinning:</strong> If the entity has a {@link Skeleton} component, this
     * method computes and uploads the joint matrices needed for vertex skinning. The skin
     * matrix formula per joint is:
     * <pre>
     *   skinMatrix[j] = jointWorldMatrix[j] * inverseBindMatrix[j]
     * </pre>
     * This transforms vertices from bind-pose space into the joint's current animated position.
     * The vertex shader then blends up to 4 joint influences per vertex using the weights
     * stored in the WEIGHTS_0 attribute.
     *
     * <p>For entities without a Skeleton, skinning is disabled (uHasSkin=false) and the
     * shader uses the raw vertex positions unchanged.
     */
    @Override
    public void prepareEntity(RenderContext ctx, EntityView entity) {
        PbrMaterial material = ctx.getRegistry().getComponent(entity.getId(), PbrMaterial.class);
        if (material == null) return;

        WorldTransform worldTransform = ctx.getRegistry().getComponent(entity.getId(), WorldTransform.class);
        Matrix4f modelMatrix = worldTransform != null
            ? worldTransform.getWorldMatrix()
            : entity.get(Transform.class).getTransformationMatrix();

        shaderSystem.setUniform(material.getShader(), UNIFORM_MODEL_MATRIX, modelMatrix);

        // --- GPU Skinning: upload joint matrices if the entity has a skeleton ---
        Skeleton skeleton = ctx.getRegistry().getComponent(entity.getId(), Skeleton.class);
        if (skeleton != null) {
            shaderSystem.setUniform(material.getShader(), UNIFORM_HAS_SKIN, true);
            int[] jointEntities = skeleton.getJointEntities();
            Matrix4f[] inverseBindMatrices = skeleton.getInverseBindMatrices();

            for (int j = 0; j < skeleton.jointCount(); j++) {
                WorldTransform jointWorld = ctx.getRegistry().getComponent(
                    jointEntities[j], WorldTransform.class);
                if (jointWorld != null) {
                    // skinMatrix = jointWorldMatrix * inverseBindMatrix
                    tempJointMatrix.set(jointWorld.getWorldMatrix()).mul(inverseBindMatrices[j]);
                } else {
                    // Joint has no world transform yet — use identity (bind pose)
                    tempJointMatrix.identity();
                }
                shaderSystem.setUniform(material.getShader(),
                    UNIFORM_JOINT_MATRICES + "[" + j + "]", tempJointMatrix);
            }
        } else {
            shaderSystem.setUniform(material.getShader(), UNIFORM_HAS_SKIN, false);
        }
    }

    @Override
    public void unapply() {
        shaderSystem.clearProgram();
    }

    /**
     * Binds a texture to a texture unit if present, and sets the corresponding
     * feature flag uniform so the shader knows whether to sample from it.
     *
     * <p>OpenGL texture units are numbered slots (GL_TEXTURE0, GL_TEXTURE1, etc.)
     * that allow multiple textures to be active simultaneously. Each sampler uniform
     * in the shader references a texture unit number (not a texture ID). By assigning
     * each PBR map to a different unit, the fragment shader can sample all of them
     * in a single draw call.
     *
     * @param shader the shader program to set uniforms on
     * @param texture the texture to bind, or null if the material doesn't use this map
     * @param unit the texture unit index (0-4) to bind to
     * @param samplerUniform the name of the sampler2D uniform in the shader
     * @param flagUniform the name of the boolean uniform indicating texture presence
     */
    private void bindTextureIfPresent(Shader shader, Texture texture, int unit,
                                      String samplerUniform, String flagUniform) {
        if (texture != null) {
            textureSystem.bind(texture, unit);
            shaderSystem.setUniform(shader, samplerUniform, unit);
            shaderSystem.setUniform(shader, flagUniform, true);
        } else {
            shaderSystem.setUniform(shader, flagUniform, false);
        }
    }
}
