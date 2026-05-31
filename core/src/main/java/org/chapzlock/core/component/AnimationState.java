package org.chapzlock.core.component;

import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.ecs.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Controls animation playback state for an entity.
 *
 * <p>Attach this component to an entity (typically the root of an animated model)
 * to play animations on it. The {@link org.chapzlock.core.animation.AnimationSystem}
 * reads this component each frame to advance time and sample keyframes.
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 * AnimationState state = new AnimationState(walkAnimation);
 * state.setLooping(true);
 * state.setSpeed(1.5f); // 1.5x speed
 * registry.addComponent(entityId, state);
 * }</pre>
 *
 * <p><strong>How it works:</strong> Each frame, AnimationSystem:
 * <ol>
 *   <li>Advances {@code time} by {@code deltaTime * speed}</li>
 *   <li>If looping, wraps time around duration; otherwise clamps to duration</li>
 *   <li>Samples animation channels at current time</li>
 *   <li>Writes sampled TRS values into target entity Transform components</li>
 * </ol>
 *
 * @see Animation
 * @see org.chapzlock.core.animation.AnimationSystem
 */
@Getter
@Setter
public class AnimationState implements Component {

    /** The animation clip currently playing. */
    private Animation currentAnimation;

    /** Current playback time in seconds (0 to animation duration). */
    private float time;

    /** Whether the animation loops back to the start when it reaches the end. */
    private boolean looping;

    /** Playback speed multiplier. 1.0 = normal, 2.0 = double speed, 0.5 = half speed. */
    private float speed;

    /** Whether the animation is currently playing (vs. paused). */
    private boolean playing;

    /**
     * Creates an animation state with the given clip, looping at normal speed.
     *
     * @param animation the animation clip to play
     */
    public AnimationState(Animation animation) {
        this.currentAnimation = animation;
        this.time = 0f;
        this.looping = true;
        this.speed = 1.0f;
        this.playing = true;
    }

    /**
     * Switches to a different animation, resetting time to 0.
     *
     * @param animation the new animation clip
     */
    public void play(Animation animation) {
        this.currentAnimation = animation;
        this.time = 0f;
        this.playing = true;
    }

    /** Pauses playback (time stops advancing). */
    public void pause() {
        this.playing = false;
    }

    /** Resumes playback from current time. */
    public void resume() {
        this.playing = true;
    }

    /** Returns true if the animation has reached its end (only meaningful if not looping). */
    public boolean isFinished() {
        return !looping && currentAnimation != null && time >= currentAnimation.duration();
    }
}
