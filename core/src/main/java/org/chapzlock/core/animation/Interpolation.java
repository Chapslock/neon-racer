package org.chapzlock.core.animation;

/**
 * How to interpolate between keyframes in an animation channel.
 *
 * <p><strong>What is interpolation?</strong> Animations define values only at specific
 * time points (keyframes). Between keyframes, the engine must compute intermediate values.
 * The interpolation mode controls how that's done:
 *
 * <ul>
 *   <li>{@link #LINEAR} — straight line between keyframes. Smooth for positions/scales,
 *       uses slerp (spherical linear interpolation) for rotations.</li>
 *   <li>{@link #STEP} — jump instantly to the next keyframe's value. No in-between.
 *       Used for on/off states or intentionally jerky motion.</li>
 *   <li>{@link #CUBICSPLINE} — smooth curves with tangent control. Produces the
 *       smoothest motion but requires additional tangent data per keyframe.</li>
 * </ul>
 */
public enum Interpolation {
    /** Linearly interpolate between keyframes (slerp for quaternions). */
    LINEAR,
    /** Hold the current keyframe value until the next one (no interpolation). */
    STEP,
    /** Cubic spline interpolation with tangent control points. */
    CUBICSPLINE
}
