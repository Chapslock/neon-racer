package org.chapzlock.core.application;

/**
 * Manages time scaling and delta time.
 *
 * <p>Tracks both scaled and unscaled time. Use {@code setScale()} for slow-motion or pause effects.
 */
public class Time {
    private float scale = 1.0f;
    private float delta;
    private float unscaledDelta;
    private float time;
    private float unscaledTime;

    /** Updates time values. Called by the engine every frame. */
    public void update(float rawDelta) {
        this.unscaledDelta = rawDelta;
        this.delta = rawDelta * scale;
        this.unscaledTime += rawDelta;
        this.time += this.delta;
    }

    /** Returns time since last frame in seconds (scaled). */
    public float getDelta() { return delta; }
    /** Returns time since last frame in seconds (unscaled, ignores time scale). */
    public float getUnscaledDelta() { return unscaledDelta; }
    /** Returns total elapsed time in seconds (scaled). */
    public float getTime() { return time; }
    /** Returns total elapsed time in seconds (unscaled, ignores time scale). */
    public float getUnscaledTime() { return unscaledTime; }
    /** Returns current time scale multiplier. */
    public float getScale() { return scale; }
    /** Returns true if time is paused (scale = 0). */
    public boolean isPaused() { return scale == 0f; }

    /**
     * Sets the time scale multiplier.
     *
     * <p>Affects {@link #getDelta()} (scaled delta) but not {@link #getUnscaledDelta()}.
     * Use for slow-motion effects (0.5 = half speed), pause (0.0), or fast-forward (2.0).
     *
     * @param scale multiplier applied to raw delta time (default 1.0, clamped to 0+)
     */
    public void setScale(float scale) { this.scale = Math.max(0f, scale); }
    /** Pauses time (sets scale to 0). */
    public void pause() { setScale(0f); }
    /** Resumes time (sets scale to 1). */
    public void resume() { setScale(1.0f); }
}
