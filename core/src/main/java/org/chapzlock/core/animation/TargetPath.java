package org.chapzlock.core.animation;

/**
 * Identifies which property of an entity an animation channel drives.
 *
 * <p><strong>How animations work:</strong> An animation is made of "channels," where
 * each channel animates one property of one entity over time. TargetPath says WHICH
 * property the channel controls.
 *
 * <p>These map directly to the glTF animation channel target paths:
 * <ul>
 *   <li>{@link #TRANSLATION} — position (vec3)</li>
 *   <li>{@link #ROTATION} — rotation (quaternion)</li>
 *   <li>{@link #SCALE} — scale (vec3)</li>
 *   <li>{@link #WEIGHTS} — morph target blend weights (float[])</li>
 * </ul>
 */
public enum TargetPath {
    /** Animates position (vec3: x, y, z). */
    TRANSLATION,
    /** Animates rotation (quaternion: x, y, z, w). */
    ROTATION,
    /** Animates scale (vec3: x, y, z). */
    SCALE,
    /** Animates morph target weights (variable-length float array). */
    WEIGHTS
}
