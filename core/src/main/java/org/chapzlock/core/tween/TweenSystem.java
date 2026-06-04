package org.chapzlock.core.tween;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.ecs.GameSystem;

/**
 * GameSystem wrapper for {@link TweenManager}.
 *
 * <p>Updates all active tweens each frame. Add to engine systems if using tweens.
 */
public class TweenSystem implements GameSystem {
    private TweenManager tweenManager;

    @Override
    public void onInit(Engine engine) {
        this.tweenManager = engine.getTweenManager();
    }

    @Override
    public void onUpdate(float deltaTime) {
        tweenManager.update(deltaTime);
    }
}
