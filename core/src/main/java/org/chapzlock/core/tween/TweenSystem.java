package org.chapzlock.core.tween;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.GameSystem;

public class TweenSystem implements GameSystem {
    private TweenManager tweenManager;

    @Override
    public void onInit(EngineContext ctx) {
        this.tweenManager = ctx.getTweenManager();
    }

    @Override
    public void onUpdate(float deltaTime) {
        tweenManager.update(deltaTime);
    }
}
