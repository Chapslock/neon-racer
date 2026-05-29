package org.chapzlock.core.tween;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.GameSystem;

public class TweenSystem implements GameSystem {
    private final TweenManager tweenManager = new TweenManager();

    public TweenManager getTweenManager() {
        return tweenManager;
    }

    @Override
    public void onInit(EngineContext ctx) {
        // No initialization needed
    }

    @Override
    public void onUpdate(float deltaTime) {
        tweenManager.update(deltaTime);
    }
}
