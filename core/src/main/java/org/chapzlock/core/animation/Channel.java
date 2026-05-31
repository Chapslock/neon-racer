package org.chapzlock.core.animation;

/**
 * A single animation channel: drives one property of one entity over time.
 *
 * <p>An animation is composed of multiple channels. For example, a walk animation
 * might have channels for:
 * <ul>
 *   <li>Hip joint → TRANSLATION (bobbing up and down)</li>
 *   <li>Hip joint → ROTATION (tilting side to side)</li>
 *   <li>Left knee → ROTATION (bending during stride)</li>
 *   <li>Right knee → ROTATION (bending during stride)</li>
 * </ul>
 *
 * <p><strong>Data layout:</strong> Keyframe times are a sorted float[] (seconds).
 * Keyframe values are a flat float[] whose interpretation depends on {@link #path()}:
 * <ul>
 *   <li>TRANSLATION: 3 floats per keyframe (x, y, z)</li>
 *   <li>ROTATION: 4 floats per keyframe (x, y, z, w quaternion)</li>
 *   <li>SCALE: 3 floats per keyframe (x, y, z)</li>
 *   <li>WEIGHTS: N floats per keyframe (one per morph target)</li>
 * </ul>
 *
 * @see Animation
 * @see KeyframeSampler
 */
public record Channel(
    /** Entity ID of the node/joint this channel animates. Set during scene instantiation. */
    int targetEntity,
    /** Which transform property this channel drives. */
    TargetPath path,
    /** How to interpolate between keyframes. */
    Interpolation interpolation,
    /** Sorted array of keyframe times in seconds. */
    float[] keyframeTimes,
    /** Flat array of keyframe values (layout depends on path type). */
    float[] keyframeValues
) {
    /** Returns the number of keyframes in this channel. */
    public int keyframeCount() {
        return keyframeTimes.length;
    }

    /** Returns how many float components each keyframe has (3 for vec3, 4 for quat). */
    public int componentsPerKeyframe() {
        return switch (path) {
            case TRANSLATION, SCALE -> 3;
            case ROTATION -> 4;
            case WEIGHTS -> keyframeValues.length / keyframeTimes.length;
        };
    }
}
