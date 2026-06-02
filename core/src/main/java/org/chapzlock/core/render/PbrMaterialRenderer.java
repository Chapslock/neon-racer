package org.chapzlock.core.render;

import static org.chapzlock.core.render.PbrShaderProps.*;

import java.util.Arrays;
import java.util.List;

import org.chapzlock.core.component.*;
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
public class PbrMaterialRenderer {

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

    /**
     * Reusable VBO for uploading blended morph position deltas each frame.
     * Created lazily on first use and reused across frames to avoid allocation churn.
     * Zero means not yet created.
     */
    private int morphPositionVbo = 0;

    /**
     * Reusable VBO for uploading blended morph normal deltas each frame.
     * Created lazily on first use. Zero means not yet created.
     */
    private int morphNormalVbo = 0;

    /**
     * Reusable array for accumulating blended morph position deltas.
     * Resized as needed to match the largest morphed mesh encountered.
     */
    private float[] blendedPositionDeltas;

    /**
     * Reusable array for accumulating blended morph normal deltas.
     * Resized as needed to match the largest morphed mesh encountered.
     */
    private float[] blendedNormalDeltas;

    public PbrMaterialRenderer(ShaderSystem shaderSystem, TextureSystem textureSystem) {
        this.shaderSystem = shaderSystem;
        this.textureSystem = textureSystem;
    }

    /**
     * Binds the PBR shader and sets per-batch uniforms (projection, view, lighting, material).
     *
     * <p>Call this once per material batch before rendering entities that share this material.
     * Then call {@link #prepareEntity(RenderContext, int)} for each entity to set
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
        List<RenderContext.PointLightInstance> lights = ctx.getPointLights();
        shaderSystem.setUniform(shader, UNIFORM_NUM_POINT_LIGHTS, lights.size());
        for (int i = 0; i < lights.size(); i++) {
            RenderContext.PointLightInstance pl = lights.get(i);
            shaderSystem.setUniform(shader, UNIFORM_POINT_LIGHT_POSITIONS + "[" + i + "]",
                pl.position());
            shaderSystem.setUniform(shader, UNIFORM_POINT_LIGHT_COLORS + "[" + i + "]",
                pl.color().toVector3f());
            Attenuation att = pl.attenuation();
            shaderSystem.setUniform(shader, UNIFORM_POINT_LIGHT_ATTENUATION + "[" + i + "]",
                new Vector3f(att.constant(), att.linear(), att.quadratic()));
        }
    }

    /**
     * Sets the per-entity model (world) transformation matrix and skinning uniforms.
     *
     * <p>Uses {@link Transform#getWorldMatrix()} (computed by TransformSystem for hierarchy support).
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
    public void prepareEntity(RenderContext ctx, int entityId) {
        PbrMaterial material = ctx.getRegistry().getComponent(entityId, PbrMaterial.class);
        if (material == null) return;

        Transform transform = ctx.getRegistry().getComponent(entityId, Transform.class);
        Matrix4f modelMatrix = transform != null
            ? transform.getWorldMatrix()
            : new Matrix4f();

        shaderSystem.setUniform(material.getShader(), UNIFORM_MODEL_MATRIX, modelMatrix);

        // --- GPU Skinning: upload joint matrices if the entity has a skeleton ---
        Skeleton skeleton = ctx.getRegistry().getComponent(entityId, Skeleton.class);
        if (skeleton != null) {
            shaderSystem.setUniform(material.getShader(), UNIFORM_HAS_SKIN, true);
            int[] jointEntities = skeleton.getJointEntities();
            Matrix4f[] inverseBindMatrices = skeleton.getInverseBindMatrices();

            for (int j = 0; j < skeleton.jointCount(); j++) {
                Transform jointTransform = ctx.getRegistry().getComponent(
                    jointEntities[j], Transform.class);
                if (jointTransform != null) {
                    // skinMatrix = jointWorldMatrix * inverseBindMatrix
                    tempJointMatrix.set(jointTransform.getWorldMatrix()).mul(inverseBindMatrices[j]);
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

        // --- Morph Targets: compute blended deltas on CPU and upload as vertex attributes ---
        MorphTargets morphTargets = ctx.getRegistry().getComponent(entityId, MorphTargets.class);
        Mesh mesh = ctx.getRegistry().getComponent(entityId, Mesh.class);
        if (morphTargets != null && morphTargets.getPositionDeltaArrays() != null
                && hasNonZeroWeight(morphTargets.getWeights()) && mesh != null) {
            shaderSystem.setUniform(material.getShader(), UNIFORM_HAS_MORPH_TARGETS, true);
            uploadBlendedMorphDeltas(mesh, morphTargets);
        } else {
            shaderSystem.setUniform(material.getShader(), UNIFORM_HAS_MORPH_TARGETS, false);
            // Disable morph attribute slots to prevent stale data from a previous entity
            if (mesh != null) {
                MeshUtil.disableMorphAttributes(mesh.getVaoId(),
                    ATTRIB_MORPH_POSITION_DELTA, ATTRIB_MORPH_NORMAL_DELTA);
            }
        }
    }

    public void unapply() {
        shaderSystem.clearProgram();
    }

    /**
     * Checks whether any morph target weight is non-zero.
     *
     * <p>Used to skip the blending computation entirely when all weights are zero
     * (the common case for entities that aren't currently morphing). This avoids
     * unnecessary VBO uploads and attribute binds.
     *
     * @param weights the morph target weight array
     * @return true if at least one weight is non-zero
     */
    private boolean hasNonZeroWeight(float[] weights) {
        if (weights == null) return false;
        for (float w : weights) {
            if (w != 0f) return true;
        }
        return false;
    }

    /**
     * Computes the weighted sum of all morph target position/normal deltas on the CPU,
     * then uploads the blended result as vertex attributes at slots 8 and 9.
     *
     * <p><strong>How this works:</strong>
     * <ol>
     *   <li>For each morph target with non-zero weight, accumulate:
     *       {@code blended[i] += weight * delta[i]} for all vertex components</li>
     *   <li>Upload the single blended result to a reusable dynamic VBO</li>
     *   <li>Bind the VBO as a vertex attribute in the entity's VAO</li>
     * </ol>
     *
     * <p><strong>Performance characteristics:</strong>
     * <ul>
     *   <li>CPU cost: O(activeTargets * vertexCount) per entity per frame</li>
     *   <li>Acceptable for typical use (facial animation: ~50 targets but only 2-5 active,
     *       meshes with 1k-10k vertices)</li>
     *   <li>The float[] arrays are reused across frames to avoid allocation</li>
     *   <li>The VBOs are reused (same GL IDs) with GL_DYNAMIC_DRAW</li>
     * </ul>
     *
     * @param mesh the entity's mesh component (provides VAO ID)
     * @param morphTargets the morph targets component with weights and delta arrays
     */
    private void uploadBlendedMorphDeltas(Mesh mesh, MorphTargets morphTargets) {
        float[][] posArrays = morphTargets.getPositionDeltaArrays();
        float[] weights = morphTargets.getWeights();
        int floatCount = posArrays[0].length; // vertexCount * 3

        // Ensure reusable array is large enough
        if (blendedPositionDeltas == null || blendedPositionDeltas.length < floatCount) {
            blendedPositionDeltas = new float[floatCount];
        } else {
            Arrays.fill(blendedPositionDeltas, 0, floatCount, 0f);
        }

        // Accumulate weighted position deltas
        int targetCount = Math.min(morphTargets.getTargetCount(), MAX_MORPH_TARGETS);
        for (int t = 0; t < targetCount; t++) {
            float w = weights[t];
            if (w == 0f) continue;
            float[] deltas = posArrays[t];
            for (int i = 0; i < floatCount; i++) {
                blendedPositionDeltas[i] += w * deltas[i];
            }
        }

        // Upload blended position deltas to VBO at attribute slot 8
        if (morphPositionVbo == 0) {
            morphPositionVbo = MeshUtil.createDynamicVbo();
        }
        MeshUtil.bindBlendedMorphDelta(mesh.getVaoId(), ATTRIB_MORPH_POSITION_DELTA,
            morphPositionVbo, copyRange(blendedPositionDeltas, floatCount));

        // Normal deltas (optional)
        float[][] normalArrays = morphTargets.getNormalDeltaArrays();
        if (normalArrays != null) {
            if (blendedNormalDeltas == null || blendedNormalDeltas.length < floatCount) {
                blendedNormalDeltas = new float[floatCount];
            } else {
                Arrays.fill(blendedNormalDeltas, 0, floatCount, 0f);
            }

            for (int t = 0; t < targetCount; t++) {
                float w = weights[t];
                if (w == 0f || normalArrays[t] == null) continue;
                float[] nDeltas = normalArrays[t];
                for (int i = 0; i < floatCount; i++) {
                    blendedNormalDeltas[i] += w * nDeltas[i];
                }
            }

            if (morphNormalVbo == 0) {
                morphNormalVbo = MeshUtil.createDynamicVbo();
            }
            MeshUtil.bindBlendedMorphDelta(mesh.getVaoId(), ATTRIB_MORPH_NORMAL_DELTA,
                morphNormalVbo, copyRange(blendedNormalDeltas, floatCount));
        } else {
            // No normal deltas — upload zeros for normal slot so the shader reads (0,0,0)
            // which has no effect when added to the base normal
            if (blendedNormalDeltas == null || blendedNormalDeltas.length < floatCount) {
                blendedNormalDeltas = new float[floatCount];
            } else {
                Arrays.fill(blendedNormalDeltas, 0, floatCount, 0f);
            }
            if (morphNormalVbo == 0) {
                morphNormalVbo = MeshUtil.createDynamicVbo();
            }
            MeshUtil.bindBlendedMorphDelta(mesh.getVaoId(), ATTRIB_MORPH_NORMAL_DELTA,
                morphNormalVbo, copyRange(blendedNormalDeltas, floatCount));
        }
    }

    /**
     * Returns a correctly-sized copy of the reusable buffer up to the given length.
     * This avoids uploading trailing zeros when the reusable array is larger than needed.
     */
    private float[] copyRange(float[] source, int length) {
        if (source.length == length) return source;
        return Arrays.copyOf(source, length);
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
