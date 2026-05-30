package org.chapzlock.core.tween;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages active tweens (animations).
 *
 * <p>Create via {@link #create(float, Ease, Consumer)}, update via {@link #update(float)}.
 * Automatically removes completed or cancelled tweens.
 */
public class TweenManager {
    private final List<Tween> activeTweens = new ArrayList<>();

    /**
     * Creates and starts a new tween.
     *
     * @param duration tween duration in seconds
     * @param ease easing function
     * @param progressCallback called each frame with 0..1 progress (after easing)
     * @return the tween (for chaining {@code .onComplete()})
     */
    public Tween create(float duration, Ease ease, Consumer<Float> progressCallback) {
        Tween tween = new Tween(duration, ease, progressCallback);
        activeTweens.add(tween);
        return tween;
    }

    public void update(float delta) {
        Iterator<Tween> it = activeTweens.iterator();
        while (it.hasNext()) {
            if (it.next().update(delta)) {
                it.remove();
            }
        }
    }

    public int getActiveCount() { return activeTweens.size(); }
}
