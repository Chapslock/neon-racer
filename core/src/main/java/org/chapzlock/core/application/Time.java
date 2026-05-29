package org.chapzlock.core.application;

public class Time {
    private float scale = 1.0f;
    private float delta;
    private float unscaledDelta;
    private float time;
    private float unscaledTime;

    public void update(float rawDelta) {
        this.unscaledDelta = rawDelta;
        this.delta = rawDelta * scale;
        this.unscaledTime += rawDelta;
        this.time += this.delta;
    }

    public float getDelta() { return delta; }
    public float getUnscaledDelta() { return unscaledDelta; }
    public float getTime() { return time; }
    public float getUnscaledTime() { return unscaledTime; }
    public float getScale() { return scale; }
    public boolean isPaused() { return scale == 0f; }

    public void setScale(float scale) { this.scale = Math.max(0f, scale); }
    public void pause() { setScale(0f); }
    public void resume() { setScale(1.0f); }
}
