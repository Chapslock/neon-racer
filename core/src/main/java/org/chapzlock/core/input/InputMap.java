package org.chapzlock.core.input;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.input.keyboard.KeyPressedEvent;
import org.chapzlock.core.input.keyboard.KeyReleasedEvent;
import org.chapzlock.core.logging.Log;

/**
 * Input mapping system for actions and axes.
 *
 * <p>Actions are discrete (pressed/released). Axes are continuous (-1 to +1).
 * Build via {@link InputMapBuilder}, activate via {@link #activate(EventBus)} (typically in scene load),
 * deactivate via {@link #deactivate()} (typically in scene unload).
 *
 * <p>Example:
 * <pre>{@code
 * InputMap inputMap = InputMap.builder()
 *     .action("jump", Key.SPACE)
 *     .axis("move", Key.A, Key.D)
 *     .build();
 * inputMap.activate(ctx.getEventBus());
 *
 * if (inputMap.wasJustPressed("jump")) {
 *     // Handle jump
 * }
 * float move = inputMap.getAxis("move");
 * }</pre>
 */
public class InputMap {
    private final Map<String, List<Key>> actionBindings;
    private final Map<String, Key[]> axisBindings;

    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> justPressedKeys = new HashSet<>();
    private final Set<Integer> justReleasedKeys = new HashSet<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final Set<String> warnedActions = new HashSet<>();

    InputMap(Map<String, List<Key>> actionBindings, Map<String, Key[]> axisBindings) {
        this.actionBindings = actionBindings;
        this.axisBindings = axisBindings;
    }

    /** Creates a new input map builder. */
    public static InputMapBuilder builder() {
        return new InputMapBuilder();
    }

    /**
     * Activates this input map, subscribing to key events. Called by {@link org.chapzlock.core.scene.SceneManager} on scene load.
     *
     * @param eventBus the event bus
     */
    public void activate(EventBus eventBus) {
        subscriptions.add(eventBus.subscribe(KeyPressedEvent.class, this::handleKeyPressed));
        subscriptions.add(eventBus.subscribe(KeyReleasedEvent.class, this::handleKeyReleased));
        Log.debug("InputMap activated with actions: " + actionBindings.keySet() + ", axes: " + axisBindings.keySet());
    }

    /**
     * Deactivates this input map, unsubscribing from key events. Called by {@link org.chapzlock.core.scene.SceneManager} on scene unload.
     */
    public void deactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pressedKeys.clear();
        justPressedKeys.clear();
        justReleasedKeys.clear();
        Log.debug("InputMap deactivated");
    }

    public void frameReset() {
        justPressedKeys.clear();
        justReleasedKeys.clear();
    }

    public boolean isPressed(String action) {
        if (isUnregisteredAction(action)) return false;
        List<Key> keys = actionBindings.get(action);
        for (Key key : keys) {
            if (pressedKeys.contains(key.getGlfwCode())) return true;
        }
        return false;
    }

    public boolean wasJustPressed(String action) {
        if (isUnregisteredAction(action)) return false;
        List<Key> keys = actionBindings.get(action);
        for (Key key : keys) {
            if (justPressedKeys.contains(key.getGlfwCode())) return true;
        }
        return false;
    }

    public boolean wasJustReleased(String action) {
        if (isUnregisteredAction(action)) return false;
        List<Key> keys = actionBindings.get(action);
        for (Key key : keys) {
            if (justReleasedKeys.contains(key.getGlfwCode())) return true;
        }
        return false;
    }

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

    private boolean isUnregisteredAction(String action) {
        if (actionBindings.containsKey(action)) return false;
        if (warnedActions.add(action)) {
            Log.warn("InputMap: queried unregistered action \"" + action +
                "\". Registered actions: " + actionBindings.keySet());
        }
        return true;
    }

    private boolean isUnregisteredAxis(String axisName) {
        if (axisBindings.containsKey(axisName)) return false;
        if (warnedActions.add(axisName)) {
            Log.warn("InputMap: queried unregistered axis \"" + axisName +
                "\". Registered axes: " + axisBindings.keySet());
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
}
