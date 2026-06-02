package org.chapzlock.core.component;

import java.util.Map;
import java.util.Set;

import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.ecs.Component;

/**
 * Holds all animation clips available on an entity, keyed by name.
 *
 * <p><strong>What is this?</strong> When a glTF model is loaded, it may contain multiple
 * animation clips (e.g., "Walk", "Run", "Idle", "Attack"). This component stores all
 * of them on the entity so game code can switch between animations at runtime.
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 * AnimationClipSet clips = registry.getComponent(entityId, AnimationClipSet.class);
 * Animation run = clips.get("Run");
 * AnimationState state = registry.getComponent(entityId, AnimationState.class);
 * state.play(run, 0.2f); // crossfade to "Run" over 200ms
 * }</pre>
 *
 * <p><strong>Lifecycle:</strong> Created by {@link org.chapzlock.core.scene.GltfModelFactory}
 * during Phase 5 (animation instantiation). The clips are shared immutable data — multiple
 * entities instantiated from the same model reference the same Animation objects.
 *
 * @see Animation
 * @see AnimationState
 */
public class AnimationClipSet implements Component {

    private final Map<String, Animation> clips;

    /**
     * Creates a clip set from a name-to-animation map.
     *
     * @param clips all animation clips keyed by their glTF name
     */
    public AnimationClipSet(Map<String, Animation> clips) {
        this.clips = Map.copyOf(clips);
    }

    /**
     * Returns the animation clip with the given name, or null if not found.
     *
     * @param name the clip name (e.g., "Walk", "Idle") — case-sensitive
     */
    public Animation get(String name) {
        return clips.get(name);
    }

    /**
     * Returns the set of all available clip names.
     */
    public Set<String> clipNames() {
        return clips.keySet();
    }

    /**
     * Returns the number of clips in this set.
     */
    public int size() {
        return clips.size();
    }
}
