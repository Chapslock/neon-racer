package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

/**
 * Stores morph target (blend shape) data for an entity's mesh.
 *
 * <p><strong>What are morph targets?</strong> Morph targets (also called "blend shapes") let you
 * smoothly deform a mesh between predefined shapes without needing a skeleton. Each morph target
 * stores a set of vertex position/normal deltas — the difference between the base mesh and a
 * deformed version. By blending multiple targets together with different weights you can create
 * complex deformations from simple building blocks.
 *
 * <p><strong>Common use cases:</strong>
 * <ul>
 *   <li>Facial animation — smile, frown, blink, lip sync phonemes</li>
 *   <li>Muscle bulging when limbs bend (corrective blend shapes)</li>
 *   <li>Damage deformation — progressive dents on a vehicle</li>
 *   <li>Character customization — nose width, brow height</li>
 * </ul>
 *
 * <p><strong>How weights control blending:</strong> Each morph target has a weight between 0.0 and
 * 1.0 (though values outside this range are legal for exaggerated effects). A weight of 0.0 means
 * the target has no effect; 1.0 means the full delta is applied. Multiple targets can be active
 * simultaneously, and their effects are additive.
 *
 * <p><strong>Integration with AnimationSystem:</strong> The {@code AnimationSystem} drives morph
 * weights over time via animation channels targeting {@code TargetPath.WEIGHTS}. Each keyframe
 * stores a full set of weights, and the sampler interpolates between keyframes to produce smooth
 * transitions. The sampled weights are written directly into this component every frame.
 *
 * <p><strong>How the shader uses this:</strong> The vertex shader receives the current weights as
 * a uniform array and the per-target position/normal deltas as additional vertex attributes (one
 * VBO per target). It computes the final vertex position as:
 * {@code finalPos = basePos + weight[0]*delta[0] + weight[1]*delta[1] + ...}
 *
 * @see Skeleton
 */
public class MorphTargets implements Component {

    /** Current blend weights for each morph target. Animated by AnimationSystem. */
    private final float[] weights;

    /** Number of morph targets. */
    private final int targetCount;

    /**
     * OpenGL VBO IDs for morph target position deltas.
     * One VBO per target, each containing vec3 deltas for every vertex.
     * Null until GPU upload happens.
     */
    private int[] positionDeltaVbos;

    /**
     * OpenGL VBO IDs for morph target normal deltas (optional).
     * Null if no normal deltas are present.
     */
    private int[] normalDeltaVbos;

    /**
     * Raw CPU-side position delta arrays for CPU blending.
     *
     * <p>Each entry is a flat float array containing vec3 deltas for every vertex in the mesh
     * (length = vertexCount * 3). The outer array has one entry per morph target. These are
     * retained in RAM so that {@code PbrMaterialRenderer} can compute the weighted sum of
     * active targets each frame without reading back from GPU memory.
     *
     * <p>Memory layout per target: [dx0, dy0, dz0, dx1, dy1, dz1, ...] where vertex i
     * occupies indices [i*3, i*3+1, i*3+2].
     *
     * <p>Set alongside VBOs during mesh upload. May be null if the mesh loader doesn't
     * retain CPU data.
     */
    private float[][] positionDeltaArrays;

    /**
     * Raw CPU-side normal delta arrays for CPU blending (optional).
     *
     * <p>Same layout as {@link #positionDeltaArrays} but for normal vector deltas.
     * May be null if the morph targets only affect positions.
     */
    private float[][] normalDeltaArrays;

    /**
     * Creates a MorphTargets component with the given number of targets.
     * All weights start at 0.0 (no deformation applied).
     *
     * @param targetCount number of morph targets this mesh has
     */
    public MorphTargets(int targetCount) {
        this.targetCount = targetCount;
        this.weights = new float[targetCount];
    }

    /**
     * Creates a MorphTargets component with initial weights.
     * The provided array is copied, so subsequent modifications to the source array
     * will not affect this component.
     *
     * @param initialWeights the starting weights (array is copied)
     */
    public MorphTargets(float[] initialWeights) {
        this.targetCount = initialWeights.length;
        this.weights = new float[targetCount];
        System.arraycopy(initialWeights, 0, this.weights, 0, targetCount);
    }

    /** Returns the number of morph targets. */
    public int getTargetCount() {
        return targetCount;
    }

    /**
     * Returns the current weight array. This is the live array — modifications to it will
     * directly affect blending. Prefer {@link #setWeight(int, float)} or
     * {@link #setWeights(float[])} for clarity.
     */
    public float[] getWeights() {
        return weights;
    }

    /**
     * Sets a single morph target weight.
     *
     * @param index the morph target index (0-based)
     * @param weight the blend weight (typically 0.0 to 1.0)
     */
    public void setWeight(int index, float weight) {
        weights[index] = weight;
    }

    /**
     * Copies weights from the source array into this component's weights.
     * If the source array is shorter than the target count, only the first
     * {@code source.length} weights are updated. If longer, excess values are ignored.
     *
     * @param source array of weights to copy from
     */
    public void setWeights(float[] source) {
        System.arraycopy(source, 0, weights, 0, Math.min(source.length, targetCount));
    }

    /**
     * Returns the OpenGL VBO IDs for position delta buffers (one per morph target),
     * or null if not yet uploaded to the GPU.
     */
    public int[] getPositionDeltaVbos() {
        return positionDeltaVbos;
    }

    /**
     * Sets the OpenGL VBO IDs for position delta buffers.
     * Called by the mesh upload system after GPU resources are allocated.
     *
     * @param vbos array of VBO IDs, one per morph target
     */
    public void setPositionDeltaVbos(int[] vbos) {
        this.positionDeltaVbos = vbos;
    }

    /**
     * Returns the OpenGL VBO IDs for normal delta buffers (one per morph target),
     * or null if no normal deltas are present.
     */
    public int[] getNormalDeltaVbos() {
        return normalDeltaVbos;
    }

    /**
     * Sets the OpenGL VBO IDs for normal delta buffers.
     * Called by the mesh upload system after GPU resources are allocated.
     * May remain null if the morph targets only affect positions.
     *
     * @param vbos array of VBO IDs, one per morph target (or null)
     */
    public void setNormalDeltaVbos(int[] vbos) {
        this.normalDeltaVbos = vbos;
    }

    /**
     * Returns the raw CPU-side position delta arrays for CPU blending,
     * or null if not retained.
     *
     * @return array of float arrays (one per morph target), or null
     */
    public float[][] getPositionDeltaArrays() {
        return positionDeltaArrays;
    }

    /**
     * Sets the raw CPU-side position delta arrays for CPU blending.
     *
     * <p>Should be called during mesh loading alongside VBO upload, passing the same
     * delta data that was uploaded to the GPU. Each inner array must have length
     * vertexCount * 3 (flat vec3 per vertex).
     *
     * @param arrays array of float arrays (one per morph target)
     */
    public void setPositionDeltaArrays(float[][] arrays) {
        this.positionDeltaArrays = arrays;
    }

    /**
     * Returns the raw CPU-side normal delta arrays for CPU blending,
     * or null if not present.
     *
     * @return array of float arrays (one per morph target), or null
     */
    public float[][] getNormalDeltaArrays() {
        return normalDeltaArrays;
    }

    /**
     * Sets the raw CPU-side normal delta arrays for CPU blending.
     *
     * <p>Same layout as position delta arrays. May remain null if morphs
     * only affect positions.
     *
     * @param arrays array of float arrays (one per morph target), or null
     */
    public void setNormalDeltaArrays(float[][] arrays) {
        this.normalDeltaArrays = arrays;
    }
}
