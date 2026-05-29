package org.chapzlock.core.tween;

import java.util.function.Consumer;

public class Tween {
    private final float duration;
    private final Ease ease;
    private final Consumer<Float> progressCallback;
    private Runnable onCompleteCallback;
    private float elapsed = 0f;
    private boolean cancelled = false;
    private boolean complete = false;

    Tween(float duration, Ease ease, Consumer<Float> progressCallback) {
        this.duration = duration;
        this.ease = ease;
        this.progressCallback = progressCallback;
    }

    public Tween onComplete(Runnable callback) {
        this.onCompleteCallback = callback;
        return this;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isComplete() { return complete; }
    public boolean isActive() { return !complete && !cancelled; }

    boolean update(float delta) {
        if (cancelled || complete) return true;

        elapsed += delta;
        float rawT = duration <= 0f ? 1.0f : Math.min(elapsed / duration, 1.0f);
        float easedT = ease.apply(rawT);
        progressCallback.accept(easedT);

        if (rawT >= 1.0f) {
            complete = true;
            if (onCompleteCallback != null) {
                onCompleteCallback.run();
            }
            return true;
        }
        return false;
    }
}
