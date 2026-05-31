package org.chapzlock.core.animation;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.AnimationState;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityView;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Advances animation time and samples keyframes into entity Transforms each frame.
 *
 * <p><strong>What this system does:</strong> Every frame, it finds all entities that have
 * an {@link AnimationState} component (meaning they are "animated"). For each one, it:
 * <ol>
 *   <li>Advances the animation clock by the frame's delta time</li>
 *   <li>Handles looping (wrap time) or clamping (stop at end)</li>
 *   <li>Reads the animation's channels to determine which entities/properties to update</li>
 *   <li>Samples each channel at the current time using {@link KeyframeSampler}</li>
 *   <li>Writes the sampled values (position, rotation, scale) into target entity Transforms</li>
 * </ol>
 *
 * <p><strong>Why this exists:</strong> Animations in glTF are stored as keyframe data —
 * discrete snapshots of positions/rotations at specific times. Something needs to "play"
 * that data back at runtime by interpolating between keyframes and pushing the results
 * into the ECS. That's this system's job.
 *
 * <p><strong>Execution phase:</strong> Runs in {@link SystemPhase#LOGIC} so that animated
 * transforms are up-to-date before physics and rendering run later in the frame.
 *
 * <p><strong>Performance:</strong> Reuses temporary Vector3f and Quaternionf objects as
 * fields to avoid per-frame heap allocations during the update loop.
 *
 * @see AnimationState
 * @see Animation
 * @see KeyframeSampler
 */
public class AnimationSystem implements GameSystem {

    private ComponentRegistry registry;

    /** Reusable temp vector for sampling translation and scale channels. */
    private final Vector3f tempVec3 = new Vector3f();

    /** Reusable temp quaternion for sampling rotation channels. */
    private final Quaternionf tempQuat = new Quaternionf();

    @Override
    public SystemPhase phase() {
        return SystemPhase.LOGIC;
    }

    /**
     * Stores the component registry reference for entity queries during update.
     *
     * @param ctx the engine context containing all subsystems
     */
    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    /**
     * Advances animation time and samples keyframes for all animated entities.
     *
     * <p>For each entity with an {@link AnimationState}:
     * <ul>
     *   <li>Skips if not playing or has no current animation</li>
     *   <li>Advances time by {@code deltaTime * speed}</li>
     *   <li>Wraps (looping) or clamps (non-looping) time to animation duration</li>
     *   <li>Iterates each channel and writes sampled values to target Transforms</li>
     * </ul>
     *
     * @param deltaTime time elapsed since the last frame, in seconds
     */
    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView view : registry.view(AnimationState.class)) {
            AnimationState state = view.get(AnimationState.class);

            if (!state.isPlaying() || state.getCurrentAnimation() == null) {
                continue;
            }

            Animation animation = state.getCurrentAnimation();
            float duration = animation.duration();

            // Advance the animation clock
            float time = state.getTime() + deltaTime * state.getSpeed();

            // Handle looping vs. clamping
            if (state.isLooping()) {
                if (duration > 0f) {
                    time = time % duration;
                    if (time < 0f) {
                        time += duration;
                    }
                }
            } else {
                time = Math.max(0f, Math.min(time, duration));
            }

            state.setTime(time);

            // Sample each channel and write into target entity transforms
            for (Channel channel : animation.channels()) {
                Transform target = registry.getComponent(channel.targetEntity(), Transform.class);
                if (target == null) {
                    continue;
                }

                switch (channel.path()) {
                    case TRANSLATION -> {
                        KeyframeSampler.sampleVec3(channel, time, tempVec3);
                        target.setPosition(tempVec3);
                    }
                    case ROTATION -> {
                        KeyframeSampler.sampleQuaternion(channel, time, tempQuat);
                        target.setRotationQuaternion(tempQuat);
                    }
                    case SCALE -> {
                        KeyframeSampler.sampleVec3(channel, time, tempVec3);
                        target.setScale(tempVec3.x);
                    }
                    case WEIGHTS -> {
                        // Morph targets are not yet supported (Phase 7)
                    }
                }
            }
        }
    }
}
