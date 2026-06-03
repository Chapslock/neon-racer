package org.chapzlock.core.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.input.keyboard.KeyPressedEvent;
import org.chapzlock.core.input.keyboard.KeyReleasedEvent;
import org.chapzlock.core.input.mouse.MouseButtonPressedEvent;
import org.chapzlock.core.input.mouse.MouseButtonReleasedEvent;
import org.chapzlock.core.input.mouse.MouseMovedEvent;
import org.chapzlock.core.logging.Log;

/**
 * Input mapping system for actions and axes, supporting both keyboard and mouse input.
 *
 * <p>Actions are discrete (pressed/released) and can be bound to keyboard keys or mouse buttons.
 * Keyboard axes are continuous (-1 to +1). Mouse axes report accumulated deltas per frame.
 * Build via {@link InputMapBuilder}, activate via {@link #activate(EventBus)} (typically in scene load),
 * deactivate via {@link #deactivate()} (typically in scene unload).
 *
 * <p>Example:
 * <pre>{@code
 * InputMap inputMap = InputMap.builder()
 *     .action("jump", Key.SPACE)
 *     .action("shoot", MouseButton.LEFT)
 *     .axis("move", Key.A, Key.D)
 *     .mouseAxis("lookX", "lookY")
 *     .build();
 * inputMap.activate(ctx.getEventBus());
 *
 * if (inputMap.wasJustPressed("jump")) { ... }
 * if (inputMap.wasJustPressed("shoot")) { ... }
 * float move = inputMap.getAxis("move");
 * float lookX = inputMap.getMouseAxis("lookX");
 * }</pre>
 */
public class InputMap {
    private final Map<String, List<Key>> actionBindings;
    private final Map<String, Key[]> axisBindings;
    private final Map<String, List<MouseButton>> mouseActionBindings;
    private final String mouseXAxisName;
    private final String mouseYAxisName;

    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> justPressedKeys = new HashSet<>();
    private final Set<Integer> justReleasedKeys = new HashSet<>();

    private final Set<Integer> pressedMouseButtons = new HashSet<>();
    private final Set<Integer> justPressedMouseButtons = new HashSet<>();
    private final Set<Integer> justReleasedMouseButtons = new HashSet<>();

    private final Map<String, Float> mouseAxisDeltas = new HashMap<>();
    private double lastMouseX = Double.NaN;
    private double lastMouseY = Double.NaN;
    private boolean hasMouseBaseline = false;

    private final List<Subscription> subscriptions = new ArrayList<>();
    private final Set<String> warnedActions = new HashSet<>();
    private final Set<String> warnedAxes = new HashSet<>();
    private final Set<String> warnedMouseAxes = new HashSet<>();

    InputMap(Map<String, List<Key>> actionBindings, Map<String, Key[]> axisBindings,
             Map<String, List<MouseButton>> mouseActionBindings,
             String mouseXAxisName, String mouseYAxisName) {
        this.actionBindings = actionBindings;
        this.axisBindings = axisBindings;
        this.mouseActionBindings = mouseActionBindings;
        this.mouseXAxisName = mouseXAxisName;
        this.mouseYAxisName = mouseYAxisName;
    }

    /** Creates a new input map builder. */
    public static InputMapBuilder builder() {
        return new InputMapBuilder();
    }

    /**
     * Activates this input map, subscribing to keyboard and mouse events.
     * Called by {@link org.chapzlock.core.scene.SceneManager} on scene load.
     *
     * @param eventBus the event bus
     */
    public void activate(EventBus eventBus) {
        subscriptions.add(eventBus.subscribe(KeyPressedEvent.class, this::handleKeyPressed));
        subscriptions.add(eventBus.subscribe(KeyReleasedEvent.class, this::handleKeyReleased));
        subscriptions.add(eventBus.subscribe(MouseButtonPressedEvent.class, this::handleMouseButtonPressed));
        subscriptions.add(eventBus.subscribe(MouseButtonReleasedEvent.class, this::handleMouseButtonReleased));
        subscriptions.add(eventBus.subscribe(MouseMovedEvent.class, this::handleMouseMoved));
        Log.debug("InputMap activated with actions: " + actionBindings.keySet() +
            ", mouseActions: " + mouseActionBindings.keySet() +
            ", axes: " + axisBindings.keySet() +
            (mouseXAxisName != null ? ", mouseAxes: [" + mouseXAxisName + ", " + mouseYAxisName + "]" : ""));
    }

    /**
     * Deactivates this input map, unsubscribing from all events.
     * Called by {@link org.chapzlock.core.scene.SceneManager} on scene unload.
     */
    public void deactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pressedKeys.clear();
        justPressedKeys.clear();
        justReleasedKeys.clear();
        pressedMouseButtons.clear();
        justPressedMouseButtons.clear();
        justReleasedMouseButtons.clear();
        mouseAxisDeltas.clear();
        hasMouseBaseline = false;
        lastMouseX = Double.NaN;
        lastMouseY = Double.NaN;
        Log.debug("InputMap deactivated");
    }

    /**
     * Resets per-frame input state. Call once per frame after processing input.
     * Clears justPressed/justReleased flags for both keyboard and mouse,
     * and zeroes out accumulated mouse axis deltas.
     */
    public void frameReset() {
        justPressedKeys.clear();
        justReleasedKeys.clear();
        justPressedMouseButtons.clear();
        justReleasedMouseButtons.clear();
        mouseAxisDeltas.clear();
    }

    /**
     * Returns whether the given action is currently held (keyboard key or mouse button).
     *
     * @param action the action name
     * @return true if any bound key or mouse button is currently pressed
     */
    public boolean isPressed(String action) {
        if (isUnregisteredAction(action)) return false;
        List<Key> keys = actionBindings.getOrDefault(action, Collections.emptyList());
        for (Key key : keys) {
            if (pressedKeys.contains(key.getGlfwCode())) return true;
        }
        List<MouseButton> buttons = mouseActionBindings.getOrDefault(action, Collections.emptyList());
        for (MouseButton button : buttons) {
            if (pressedMouseButtons.contains(button.getGlfwCode())) return true;
        }
        return false;
    }

    /**
     * Returns whether the given action was just pressed this frame (keyboard key or mouse button).
     *
     * @param action the action name
     * @return true if any bound key or mouse button was pressed this frame
     */
    public boolean wasJustPressed(String action) {
        if (isUnregisteredAction(action)) return false;
        List<Key> keys = actionBindings.getOrDefault(action, Collections.emptyList());
        for (Key key : keys) {
            if (justPressedKeys.contains(key.getGlfwCode())) return true;
        }
        List<MouseButton> buttons = mouseActionBindings.getOrDefault(action, Collections.emptyList());
        for (MouseButton button : buttons) {
            if (justPressedMouseButtons.contains(button.getGlfwCode())) return true;
        }
        return false;
    }

    /**
     * Returns whether the given action was just released this frame (keyboard key or mouse button).
     *
     * @param action the action name
     * @return true if any bound key or mouse button was released this frame
     */
    public boolean wasJustReleased(String action) {
        if (isUnregisteredAction(action)) return false;
        List<Key> keys = actionBindings.getOrDefault(action, Collections.emptyList());
        for (Key key : keys) {
            if (justReleasedKeys.contains(key.getGlfwCode())) return true;
        }
        List<MouseButton> buttons = mouseActionBindings.getOrDefault(action, Collections.emptyList());
        for (MouseButton button : buttons) {
            if (justReleasedMouseButtons.contains(button.getGlfwCode())) return true;
        }
        return false;
    }

    /**
     * Returns the keyboard axis value for the given axis name (-1, 0, or +1).
     *
     * @param axisName the axis name
     * @return -1 if negative key is held, +1 if positive key is held, 0 if both or neither
     */
    public float getAxis(String axisName) {
        if (isUnregisteredAxis(axisName)) return 0f;
        Key[] keys = axisBindings.get(axisName);
        boolean neg = pressedKeys.contains(keys[0].getGlfwCode());
        boolean pos = pressedKeys.contains(keys[1].getGlfwCode());
        if (neg && pos) return 0f;
        if (neg) return -1f;
        if (pos) return 1f;
        return 0f;
    }

    /**
     * Returns the accumulated mouse delta for the given mouse axis this frame.
     *
     * <p>Positive X = right, positive Y = down (matching GLFW raw callback convention).
     * The delta accumulates across all {@link MouseMovedEvent}s received in a single frame
     * and resets to zero on {@link #frameReset()}.
     *
     * @param axisName the mouse axis name (as registered via {@link InputMapBuilder#mouseAxis})
     * @return the accumulated delta, or 0 if no movement or unregistered axis
     */
    public float getMouseAxis(String axisName) {
        if (isUnregisteredMouseAxis(axisName)) return 0f;
        return mouseAxisDeltas.getOrDefault(axisName, 0f);
    }

    private boolean isUnregisteredAction(String action) {
        if (actionBindings.containsKey(action) || mouseActionBindings.containsKey(action)) return false;
        if (warnedActions.add(action)) {
            Set<String> allActions = new HashSet<>(actionBindings.keySet());
            allActions.addAll(mouseActionBindings.keySet());
            Log.warn("InputMap: queried unregistered action \"" + action +
                "\". Registered actions: " + allActions);
        }
        return true;
    }

    private boolean isUnregisteredAxis(String axisName) {
        if (axisBindings.containsKey(axisName)) return false;
        if (warnedAxes.add(axisName)) {
            Log.warn("InputMap: queried unregistered axis \"" + axisName +
                "\". Registered axes: " + axisBindings.keySet());
        }
        return true;
    }

    private boolean isUnregisteredMouseAxis(String axisName) {
        if (axisName != null && (axisName.equals(mouseXAxisName) || axisName.equals(mouseYAxisName))) return false;
        if (warnedMouseAxes.add(axisName)) {
            Log.warn("InputMap: queried unregistered mouse axis \"" + axisName +
                "\". Registered mouse axes: [" + mouseXAxisName + ", " + mouseYAxisName + "]");
        }
        return true;
    }

    private boolean handleKeyPressed(KeyPressedEvent event) {
        pressedKeys.add(event.keyCode());
        justPressedKeys.add(event.keyCode());
        return false;
    }

    private boolean handleKeyReleased(KeyReleasedEvent event) {
        pressedKeys.remove(event.keyCode());
        justReleasedKeys.add(event.keyCode());
        return false;
    }

    private boolean handleMouseButtonPressed(MouseButtonPressedEvent event) {
        pressedMouseButtons.add(event.button());
        justPressedMouseButtons.add(event.button());
        return false;
    }

    private boolean handleMouseButtonReleased(MouseButtonReleasedEvent event) {
        pressedMouseButtons.remove(event.button());
        justReleasedMouseButtons.add(event.button());
        return false;
    }

    private boolean handleMouseMoved(MouseMovedEvent event) {
        if (!hasMouseBaseline) {
            lastMouseX = event.x();
            lastMouseY = event.y();
            hasMouseBaseline = true;
            return false;
        }
        double dx = event.x() - lastMouseX;
        double dy = event.y() - lastMouseY;
        lastMouseX = event.x();
        lastMouseY = event.y();

        if (mouseXAxisName != null) {
            mouseAxisDeltas.merge(mouseXAxisName, (float) dx, Float::sum);
        }
        if (mouseYAxisName != null) {
            mouseAxisDeltas.merge(mouseYAxisName, (float) dy, Float::sum);
        }
        return false;
    }
}
