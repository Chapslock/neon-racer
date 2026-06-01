package org.chapzlock.core.animation;

import java.util.List;

/**
 * A complete animation clip containing multiple channels.
 *
 * <p>An animation represents a named, time-bounded sequence of property changes.
 * For example: "Walk", "Run", "Idle", "Attack". Each animation has a duration
 * and a set of channels that drive different joints/properties.
 *
 * <p><strong>Lifecycle:</strong>
 * <ol>
 *   <li>Loaded from glTF by GltfLoader (one Animation per glTF animation)</li>
 *   <li>Stored as reusable data (shared across instances of the same model)</li>
 *   <li>Referenced by {@link org.chapzlock.core.component.AnimationState} on an entity</li>
 *   <li>Sampled each frame by AnimationSystem</li>
 * </ol>
 *
 * @see Channel
 * @see org.chapzlock.core.component.AnimationState
 */
public record Animation(
    /** Human-readable name from glTF (e.g., "Walk", "Idle"). */
    String name,
    /** Total duration of the animation in seconds. */
    float duration,
    /** All channels in this animation. */
    List<Channel> channels
) {}
