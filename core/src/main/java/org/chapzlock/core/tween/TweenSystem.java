package org.chapzlock.core.tween;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.ecs.GameSystem;

/**
 * GameSystem that advances all active tweens each frame.
 *
 * <p><strong>What is tweening?</strong> Short for "in-betweening" — programmatically animating
 * a value from start to end over a duration using an easing function. Unlike keyframe animation
 * (which is authored in a 3D tool and drives skeletal transforms), tweens are created at runtime
 * in code for effects like UI transitions, camera moves, or color fades.
 *
 * <p>Each frame, this system iterates all tweens in the {@link TweenRegistry}, advances their
 * progress by delta time, applies the easing function, calls the value setter, and removes
 * completed tweens.
 *
 * @see TweenRegistry
 * @see Ease
 * @see Tween
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
