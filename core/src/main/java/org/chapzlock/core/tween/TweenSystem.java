package org.chapzlock.core.tween;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.ecs.GameSystem;

/**
 * GameSystem wrapper for {@link TweenRegistry}.
 *
 * <p>Updates all active tweens each frame. Add to engine systems if using tweens.
 */
public class TweenSystem implements GameSystem {
    private TweenRegistry tweenRegistry;

    @Override
    public void onInit(Engine engine) {
        this.tweenRegistry = engine.getTweenRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        tweenRegistry.update(deltaTime);
    }
}
