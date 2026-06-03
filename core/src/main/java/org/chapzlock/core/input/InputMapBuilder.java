package org.chapzlock.core.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for creating input maps.
 *
 * <p>Supports binding both keyboard keys and mouse buttons as actions,
 * keyboard axes (negative/positive key pairs), and mouse delta axes.
 *
 * <p>Example:
 * <pre>{@code
 * InputMap inputMap = InputMap.builder()
 *     .action("shoot", MouseButton.LEFT)
 *     .action("jump", Key.SPACE)
 *     .axis("move", Key.A, Key.D)
 *     .mouseAxis("lookX", "lookY")
 *     .build();
 * }</pre>
 */
public class InputMapBuilder {
    private final Map<String, List<Key>> actionBindings = new HashMap<>();
    private final Map<String, Key[]> axisBindings = new HashMap<>();
    private final Map<String, List<MouseButton>> mouseActionBindings = new HashMap<>();
    private String mouseXAxisName;
    private String mouseYAxisName;

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
     * Binds an action to a mouse button.
     *
     * <p>Mouse button actions work identically to keyboard actions: they support
     * {@code isPressed}, {@code wasJustPressed}, and {@code wasJustReleased} queries.
     *
     * @param name action name
     * @param button mouse button that triggers this action
     * @return this builder
     */
    public InputMapBuilder action(String name, MouseButton button) {
        mouseActionBindings.computeIfAbsent(name, k -> new ArrayList<>()).add(button);
        return this;
    }

    /**
     * Registers mouse delta axes for tracking mouse movement.
     *
     * <p>The X axis reports horizontal mouse delta (positive = right) and the Y axis
     * reports vertical mouse delta (positive = down, matching GLFW raw callback convention).
     * Deltas accumulate across all mouse move events in a frame and reset on {@code frameReset()}.
     *
     * @param xAxisName name for the horizontal mouse delta axis
     * @param yAxisName name for the vertical mouse delta axis
     * @return this builder
     */
    public InputMapBuilder mouseAxis(String xAxisName, String yAxisName) {
        this.mouseXAxisName = xAxisName;
        this.mouseYAxisName = yAxisName;
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
            Map.copyOf(axisBindings),
            Map.copyOf(mouseActionBindings),
            mouseXAxisName,
            mouseYAxisName
        );
    }
}
