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
 * Advances animation time and samples keyframes into entity Transforms each frame,
 * with support for crossfade blending between animation clips.
 *
 * <p><strong>What this system does:</strong> Every frame, it finds all entities that have
 * an {@link AnimationState} component (meaning they are "animated"). For each one, it:
 * <ol>
 *   <li>Advances the animation clock by the frame's delta time</li>
 *   <li>Handles looping (wrap time) or clamping (stop at end)</li>
 *   <li>If a crossfade is active, samples BOTH the outgoing and incoming animations
 *       and blends their outputs together</li>
 *   <li>Otherwise, samples channels normally and writes to Transform</li>
 * </ol>
 *
 * <h3>Crossfade Blending</h3>
 *
 * <p>When an entity transitions from one animation to another (e.g., Walk to Run),
 * a sudden pose change looks jarring. Crossfade blending smoothly interpolates between
 * the outgoing clip's pose and the incoming clip's pose over a configurable duration.
 *
 * <p><strong>How blending works per frame:</strong>
 * <ol>
 *   <li>The blend factor advances: {@code blendFactor += deltaTime / blendDuration}</li>
 *   <li>Both the previous (outgoing) and current (incoming) animation times advance</li>
 *   <li>For each channel in the current animation, the system looks for a matching
 *       channel in the previous animation (same target entity + same property path)</li>
 *   <li>If a match is found, both are sampled and linearly interpolated (lerp for
 *       translation/scale, slerp for rotation)</li>
 *   <li>If no match is found (the previous clip didn't animate that property),
 *       only the current clip's value is applied</li>
 *   <li>Once blendFactor reaches 1.0, the crossfade ends</li>
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
     *   <li>If a crossfade blend is active, delegates to {@link #updateBlending}</li>
     *   <li>Otherwise, delegates to {@link #updateNormal} for standard playback</li>
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

            if (state.isBlending()) {
                updateBlending(state, deltaTime);
            } else {
                updateNormal(state, deltaTime);
            }
        }
    }

    /**
     * Handles standard (non-blending) animation playback for a single entity.
     *
     * <p>This is the simpler path: advances time, handles looping/clamping,
     * then samples each channel and writes the result directly into the target
     * entity's Transform component.
     *
     * @param state     the animation state to advance
     * @param deltaTime time elapsed since the last frame, in seconds
     */
    private void updateNormal(AnimationState state, float deltaTime) {
        Animation animation = state.getCurrentAnimation();
        float duration = animation.duration();

        // Advance the animation clock by delta * playback speed
        float time = state.getTime() + deltaTime * state.getSpeed();

        // Handle looping (wrap around) vs. clamping (stop at end)
        if (state.isLooping()) {
            if (duration > 0f) {
                time = time % duration;
                if (time < 0f) time += duration;
            }
        } else {
            time = Math.max(0f, Math.min(time, duration));
        }
        state.setTime(time);

        // Sample each channel and apply to target transforms
        for (Channel channel : animation.channels()) {
            applyChannel(channel, time);
        }
    }

    /**
     * Handles crossfade blending between two animation clips for a single entity.
     *
     * <p>During a crossfade, two animations play simultaneously:
     * <ul>
     *   <li>The "previous" (outgoing) animation continues playing from where it was</li>
     *   <li>The "current" (incoming) animation plays from the start</li>
     *   <li>Both are sampled, and their results are interpolated by the blend factor</li>
     * </ul>
     *
     * <p>The blend factor ramps linearly from 0 to 1 over the configured blend duration.
     * At factor 0.0, the output is entirely the previous animation's pose.
     * At factor 1.0, the output is entirely the current animation's pose.
     * In between, the pose is a weighted mix.
     *
     * <p>Once the blend factor reaches 1.0, the crossfade is finalized:
     * {@code blending} is set to false and the previous animation reference is cleared.
     *
     * @param state     the animation state with an active crossfade
     * @param deltaTime time elapsed since the last frame, in seconds
     */
    private void updateBlending(AnimationState state, float deltaTime) {
        float speed = state.getSpeed();

        // Advance blend factor toward 1.0 based on elapsed time
        float blendFactor = state.getBlendFactor() + deltaTime / state.getBlendDuration();
        boolean blendComplete = blendFactor >= 1.0f;
        if (blendComplete) blendFactor = 1.0f;
        state.setBlendFactor(blendFactor);

        // Advance current (incoming) animation time
        Animation current = state.getCurrentAnimation();
        float currentTime = state.getTime() + deltaTime * speed;
        if (state.isLooping() && current.duration() > 0f) {
            currentTime = currentTime % current.duration();
            if (currentTime < 0f) currentTime += current.duration();
        } else {
            currentTime = Math.max(0f, Math.min(currentTime, current.duration()));
        }
        state.setTime(currentTime);

        // Advance previous (outgoing) animation time so it doesn't freeze mid-stride
        Animation previous = state.getPreviousAnimation();
        float previousTime = state.getPreviousTime() + deltaTime * speed;
        if (state.isLooping() && previous.duration() > 0f) {
            previousTime = previousTime % previous.duration();
            if (previousTime < 0f) previousTime += previous.duration();
        } else {
            previousTime = Math.max(0f, Math.min(previousTime, previous.duration()));
        }
        state.setPreviousTime(previousTime);

        // Sample current channels, blending with matching previous channels
        for (Channel channel : current.channels()) {
            Channel prevChannel = findMatchingChannel(previous, channel.targetEntity(), channel.path());
            if (prevChannel != null) {
                applyBlendedChannel(prevChannel, previousTime, channel, currentTime, blendFactor);
            } else {
                // No matching channel in previous clip — apply current directly
                applyChannel(channel, currentTime);
            }
        }

        // Finalize blend when complete
        if (blendComplete) {
            state.setBlending(false);
            state.setPreviousAnimation(null);
        }
    }

    /**
     * Finds a channel in the given animation that matches the specified target entity and path.
     *
     * <p>During crossfade blending, we need to find the "equivalent" channel in the
     * previous animation so we can sample both and interpolate between them. A match
     * means the channel drives the same property (e.g., TRANSLATION) on the same entity
     * (e.g., the left knee joint).
     *
     * @param animation    the animation to search in
     * @param targetEntity the entity ID the channel must target
     * @param path         the target path (TRANSLATION, ROTATION, SCALE) the channel must drive
     * @return the matching channel, or {@code null} if no match was found
     */
    private Channel findMatchingChannel(Animation animation, int targetEntity, TargetPath path) {
        for (Channel ch : animation.channels()) {
            if (ch.targetEntity() == targetEntity && ch.path() == path) {
                return ch;
            }
        }
        return null;
    }

    /**
     * Samples a single animation channel at the given time and applies the result
     * directly to the target entity's Transform component.
     *
     * <p>This is used for non-blended playback, and also as a fallback during blending
     * when the previous animation doesn't have a matching channel for a given property.
     *
     * <p><strong>How each path type is applied:</strong>
     * <ul>
     *   <li>TRANSLATION: Samples a vec3 and sets it as the entity's position</li>
     *   <li>ROTATION: Samples a quaternion and sets it as the entity's rotation</li>
     *   <li>SCALE: Samples a vec3 and uses the x component as uniform scale</li>
     *   <li>WEIGHTS: Reserved for morph targets (not yet implemented)</li>
     * </ul>
     *
     * @param channel the animation channel to sample
     * @param time    the current playback time in seconds
     */
    private void applyChannel(Channel channel, float time) {
        Transform target = registry.getComponent(channel.targetEntity(), Transform.class);
        if (target == null) return;

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
                // Morph target weight sampling — future work
            }
        }
    }

    /**
     * Samples two channels (previous and current) and writes a blended result to the
     * target entity's Transform.
     *
     * <p>This is the core of crossfade blending. For translation and scale, a standard
     * linear interpolation (lerp) is used. For rotation, spherical linear interpolation
     * (slerp) is used to avoid deforming the quaternion during blending.
     *
     * <p><strong>Example:</strong> If the previous animation has the character's hand at
     * position (0, 1, 0) and the current animation has it at (0, 2, 0), with blendFactor
     * = 0.5, the result would be (0, 1.5, 0) — halfway between the two poses.
     *
     * @param prevChannel the channel from the outgoing (previous) animation
     * @param prevTime    current playback time of the previous animation, in seconds
     * @param currChannel the channel from the incoming (current) animation
     * @param currTime    current playback time of the current animation, in seconds
     * @param blendFactor interpolation weight from 0.0 (fully previous) to 1.0 (fully current)
     */
    private void applyBlendedChannel(Channel prevChannel, float prevTime,
                                      Channel currChannel, float currTime, float blendFactor) {
        Transform target = registry.getComponent(currChannel.targetEntity(), Transform.class);
        if (target == null) return;

        switch (currChannel.path()) {
            case TRANSLATION, SCALE -> {
                // Sample the previous animation's value
                KeyframeSampler.sampleVec3(prevChannel, prevTime, tempVec3);
                float px = tempVec3.x, py = tempVec3.y, pz = tempVec3.z;

                // Sample the current animation's value
                KeyframeSampler.sampleVec3(currChannel, currTime, tempVec3);

                // Lerp between previous and current
                tempVec3.set(
                    lerp(px, tempVec3.x, blendFactor),
                    lerp(py, tempVec3.y, blendFactor),
                    lerp(pz, tempVec3.z, blendFactor)
                );

                if (currChannel.path() == TargetPath.TRANSLATION) {
                    target.setPosition(tempVec3);
                } else {
                    target.setScale(tempVec3.x);
                }
            }
            case ROTATION -> {
                // Sample previous rotation
                KeyframeSampler.sampleQuaternion(prevChannel, prevTime, tempQuat);
                Quaternionf prevQuat = new Quaternionf(tempQuat);

                // Sample current rotation
                KeyframeSampler.sampleQuaternion(currChannel, currTime, tempQuat);

                // Slerp between previous and current — this ensures smooth rotation
                // blending without distortion (unlike lerp which would denormalize)
                prevQuat.slerp(tempQuat, blendFactor, tempQuat);
                target.setRotationQuaternion(tempQuat);
            }
            case WEIGHTS -> {
                // Morph target weight blending — future work
            }
        }
    }

    /**
     * Standard linear interpolation between two float values.
     *
     * <p>Returns {@code a} when {@code t = 0}, returns {@code b} when {@code t = 1},
     * and linearly blends between them for intermediate values of {@code t}.
     *
     * @param a the starting value (returned when t = 0)
     * @param b the ending value (returned when t = 1)
     * @param t the interpolation factor, typically in [0, 1]
     * @return the interpolated value: {@code a + (b - a) * t}
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
