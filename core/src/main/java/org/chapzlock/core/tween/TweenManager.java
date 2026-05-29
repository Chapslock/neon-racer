package org.chapzlock.core.tween;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class TweenManager {
    private final List<Tween> activeTweens = new ArrayList<>();

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
