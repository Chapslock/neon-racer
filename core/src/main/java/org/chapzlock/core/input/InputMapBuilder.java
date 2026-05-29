package org.chapzlock.core.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputMapBuilder {
    private final Map<String, List<Key>> actionBindings = new HashMap<>();
    private final Map<String, Key[]> axisBindings = new HashMap<>();

    public InputMapBuilder action(String name, Key... keys) {
        actionBindings.computeIfAbsent(name, k -> new ArrayList<>()).addAll(List.of(keys));
        return this;
    }

    public InputMapBuilder axis(String name, Key negativeKey, Key positiveKey) {
        axisBindings.put(name, new Key[]{ negativeKey, positiveKey });
        return this;
    }

    public InputMap build() {
        return new InputMap(
            Map.copyOf(actionBindings),
            Map.copyOf(axisBindings)
        );
    }
}
