package org.chapzlock.core.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for creating input maps.
 */
public class InputMapBuilder {
    private final Map<String, List<Key>> actionBindings = new HashMap<>();
    private final Map<String, Key[]> axisBindings = new HashMap<>();

    /**
     * Binds an action to one or more keys.
     *
     * @param name action name
     * @param keys keys that trigger this action
     * @return this builder
     */
    public InputMapBuilder action(String name, Key... keys) {
        actionBindings.computeIfAbsent(name, k -> new ArrayList<>()).addAll(List.of(keys));
        return this;
    }

    /**
     * Binds an axis to two keys (negative and positive).
     *
     * @param name axis name
     * @param negativeKey key for -1 value
     * @param positiveKey key for +1 value
     * @return this builder
     */
    public InputMapBuilder axis(String name, Key negativeKey, Key positiveKey) {
        axisBindings.put(name, new Key[]{ negativeKey, positiveKey });
        return this;
    }

    /** Builds the input map. */
    public InputMap build() {
        return new InputMap(
            Map.copyOf(actionBindings),
            Map.copyOf(axisBindings)
        );
    }
}
