package org.chapzlock.core.tween;

/**
 * Easing functions for tweens.
 *
 * <p>Transform linear 0..1 progress to accelerated/decelerated curves.
 */
public enum Ease {
    LINEAR,
    QUAD_IN,
    QUAD_OUT,
    QUAD_IN_OUT,
    CUBIC_IN,
    CUBIC_OUT,
    CUBIC_IN_OUT,
    SINE_IN,
    SINE_OUT,
    SINE_IN_OUT;

    /**
     * Applies the easing function to a linear progress value.
     *
     * @param t linear progress (0..1)
     * @return eased progress (0..1)
     */
    public float apply(float t) {
        return switch (this) {
            case LINEAR -> t;
            case QUAD_IN -> t * t;
            case QUAD_OUT -> 1f - (1f - t) * (1f - t);
            case QUAD_IN_OUT -> t < 0.5f ? 2f * t * t : 1f - ((-2f * t + 2f) * (-2f * t + 2f)) / 2f;
            case CUBIC_IN -> t * t * t;
            case CUBIC_OUT -> 1f - (1f - t) * (1f - t) * (1f - t);
            case CUBIC_IN_OUT -> t < 0.5f ? 4f * t * t * t : 1f - ((-2f * t + 2f) * (-2f * t + 2f) * (-2f * t + 2f)) / 2f;
            case SINE_IN -> 1f - (float) Math.cos(t * Math.PI / 2.0);
            case SINE_OUT -> (float) Math.sin(t * Math.PI / 2.0);
            case SINE_IN_OUT -> -((float) Math.cos(Math.PI * t) - 1f) / 2f;
        };
    }
}
