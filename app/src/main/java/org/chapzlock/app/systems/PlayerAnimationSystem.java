package org.chapzlock.app.systems;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.AnimationClipSet;
import org.chapzlock.core.component.AnimationState;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;

/**
 * Selects which animation to play on the player character based on movement input.
 *
 * <p>Each frame, reads {@link PlayerInputComponent} to determine if the player is moving,
 * then plays the corresponding animation clip ("Run" or "Idle") from the entity's
 * {@link AnimationClipSet}. Uses crossfade blending for smooth transitions between clips.
 *
 * <p>This system expects the player entity to have all four components:
 * {@link PlayerTag}, {@link PlayerInputComponent}, {@link AnimationState}, and
 * {@link AnimationClipSet}. The animation components are automatically promoted onto
 * the player entity by the scene instantiator when using {@code .gltfModel()}.
 */
public class PlayerAnimationSystem implements GameSystem {

    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_RUN = "Run";

    private ComponentRegistry registry;

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        registry.forEach(PlayerTag.class, PlayerInputComponent.class,
            AnimationState.class, AnimationClipSet.class,
            (entityId, tag, input, state, clips) -> {
                boolean moving = input.getMoveForward() != 0;
                String targetName = moving ? ANIM_RUN : ANIM_IDLE;
                Animation target = clips.get(targetName);

                if (target != null && target != state.getCurrentAnimation()) {
                    state.play(target);
                    state.setLooping(true);
                }
            });
    }
}
