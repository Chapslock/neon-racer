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

public class InputMap {
    private final Map<String, List<Key>> actionBindings;
    private final Map<String, Key[]> axisBindings;

    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> justPressedKeys = new HashSet<>();
    private final Set<Integer> justReleasedKeys = new HashSet<>();
    private final List<Subscription> subscriptions = new ArrayList<>();

    InputMap(Map<String, List<Key>> actionBindings, Map<String, Key[]> axisBindings) {
        this.actionBindings = actionBindings;
        this.axisBindings = axisBindings;
    }

    public static InputMapBuilder builder() {
        return new InputMapBuilder();
    }

    public void activate(EventBus eventBus) {
        subscriptions.add(eventBus.subscribe(KeyPressedEvent.class, this::handleKeyPressed));
        subscriptions.add(eventBus.subscribe(KeyReleasedEvent.class, this::handleKeyReleased));
    }

    public void deactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pressedKeys.clear();
        justPressedKeys.clear();
        justReleasedKeys.clear();
    }

    public void frameReset() {
        justPressedKeys.clear();
        justReleasedKeys.clear();
    }

    public boolean isPressed(String action) {
        List<Key> keys = actionBindings.get(action);
        if (keys == null) return false;
        for (Key key : keys) {
            if (pressedKeys.contains(key.getGlfwCode())) return true;
        }
        return false;
    }

    public boolean wasJustPressed(String action) {
        List<Key> keys = actionBindings.get(action);
        if (keys == null) return false;
        for (Key key : keys) {
            if (justPressedKeys.contains(key.getGlfwCode())) return true;
        }
        return false;
    }

    public boolean wasJustReleased(String action) {
        List<Key> keys = actionBindings.get(action);
        if (keys == null) return false;
        for (Key key : keys) {
            if (justReleasedKeys.contains(key.getGlfwCode())) return true;
        }
        return false;
    }

    public float getAxis(String axisName) {
        Key[] keys = axisBindings.get(axisName);
        if (keys == null) return 0f;
        boolean neg = pressedKeys.contains(keys[0].getGlfwCode());
        boolean pos = pressedKeys.contains(keys[1].getGlfwCode());
        if (neg && pos) return 0f;
        if (neg) return -1f;
        if (pos) return 1f;
        return 0f;
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
