package org.chapzlock.core.component;

import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.ecs.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Controls animation playback state for an entity, including crossfade blending.
 *
 * <p>Attach this component to an entity (typically the root of an animated model)
 * to play animations on it. The {@link org.chapzlock.core.animation.AnimationSystem}
 * reads this component each frame to advance time and sample keyframes.
 *
 * <h3>Crossfade Blending</h3>
 *
 * <p>When you switch from one animation to another (e.g., Walk to Run), a sudden
 * jump between poses looks unnatural. Crossfade blending solves this by smoothly
 * interpolating between the outgoing (previous) animation and the incoming (current)
 * animation over a short duration.
 *
 * <p>During a crossfade:
 * <ul>
 *   <li>{@code previousAnimation} holds the outgoing clip being faded out</li>
 *   <li>{@code currentAnimation} holds the incoming clip being faded in</li>
 *   <li>{@code blendFactor} ramps from 0.0 (fully previous) to 1.0 (fully current)</li>
 *   <li>The AnimationSystem samples BOTH clips and interpolates their results</li>
 *   <li>Once blendFactor reaches 1.0, the crossfade ends and only the current clip plays</li>
 * </ul>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 * AnimationState state = new AnimationState(walkAnimation);
 * state.setLooping(true);
 * state.setSpeed(1.5f); // 1.5x speed
 * registry.addComponent(entityId, state);
 *
 * // Later, switch to run with a 0.3s crossfade:
 * state.play(runAnimation, 0.3f);
 * }</pre>
 *
 * <p><strong>How it works:</strong> Each frame, AnimationSystem:
 * <ol>
 *   <li>Advances {@code time} by {@code deltaTime * speed}</li>
 *   <li>If looping, wraps time around duration; otherwise clamps to duration</li>
 *   <li>Samples animation channels at current time</li>
 *   <li>If blending, also samples previousAnimation and interpolates by blendFactor</li>
 *   <li>Writes sampled TRS values into target entity Transform components</li>
 * </ol>
 *
 * @see Animation
 * @see org.chapzlock.core.animation.AnimationSystem
 */
@Getter
@Setter
public class AnimationState implements Component {

    /** The animation clip currently playing (the "incoming" clip during a crossfade). */
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
     * The outgoing animation clip during a crossfade transition.
     *
     * <p>When a new animation is started via {@link #play(Animation, float)}, the
     * previously playing clip is stored here so the system can sample it and blend
     * its output with the new clip. This is {@code null} when no crossfade is active.
     */
    private Animation previousAnimation;

    /**
     * The playback time of the outgoing animation at the moment the crossfade began.
     *
     * <p>This captures where the old animation was so the blend starts from that
     * exact pose. During the crossfade, the system may continue advancing this time
     * so the previous animation doesn't freeze mid-stride.
     */
    private float previousTime;

    /**
     * Blend interpolation factor from 0.0 to 1.0.
     *
     * <p>At 0.0, the output pose is entirely from {@code previousAnimation}.
     * At 1.0, the output pose is entirely from {@code currentAnimation}.
     * Values in between produce a weighted mix of both poses.
     *
     * <p>The AnimationSystem advances this value each frame:
     * {@code blendFactor += deltaTime / blendDuration}.
     */
    private float blendFactor;

    /**
     * How long the crossfade transition takes, in seconds.
     *
     * <p>A value of 0.2 means the transition completes in 200 milliseconds.
     * Shorter values produce snappier transitions; longer values produce smoother ones.
     * Typical values range from 0.1 (fast) to 0.5 (very smooth).
     */
    private float blendDuration;

    /**
     * Whether a crossfade blend is currently in progress.
     *
     * <p>Set to {@code true} when {@link #play(Animation, float)} is called while
     * another animation is already playing. The AnimationSystem sets this back to
     * {@code false} once {@code blendFactor} reaches 1.0.
     */
    private boolean blending;

    /**
     * Creates an animation state with the given clip, looping at normal speed.
     *
     * <p>If the provided animation is {@code null}, the state is created in a paused
     * state (playing = false), ready to have an animation assigned later via
     * {@link #play(Animation)} or {@link #play(Animation, float)}.
     *
     * @param animation the animation clip to play, or {@code null} for an empty state
     */
    public AnimationState(Animation animation) {
        this.currentAnimation = animation;
        this.time = 0f;
        this.looping = true;
        this.speed = 1.0f;
        this.playing = animation != null;
    }

    /**
     * Switches to a new animation with a crossfade over the specified duration.
     *
     * <p>If an animation is currently playing, it becomes the "previous" animation
     * and a crossfade blend begins. The new animation starts from time 0 and
     * gradually takes over as {@code blendFactor} increases from 0 to 1.
     *
     * <p>If no animation is currently playing (e.g., state was just created with
     * {@code null}), the new animation starts immediately with no crossfade.
     *
     * @param animation     the new animation clip to play
     * @param blendDuration how long the crossfade should take, in seconds
     */
    public void play(Animation animation, float blendDuration) {
        if (currentAnimation != null && playing) {
            previousAnimation = currentAnimation;
            previousTime = time;
            blending = true;
            blendFactor = 0f;
            this.blendDuration = blendDuration;
        }
        currentAnimation = animation;
        time = 0f;
        playing = true;
    }

    /**
     * Switches to a new animation with the default crossfade duration (200ms).
     *
     * <p>This is a convenience overload of {@link #play(Animation, float)} using
     * a default blend duration of 0.2 seconds, which works well for most transitions
     * between locomotion animations (walk, run, idle).
     *
     * @param animation the new animation clip to play
     */
    public void play(Animation animation) {
        play(animation, 0.2f);
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
