package org.chapzlock.core.input.keyboard;

import org.chapzlock.core.event.Event;

/**
 * @param key       GLFW key code
 * @param modifiers GLFW modifiers for the key
 */
public record KeyPressedEvent(int key, int modifiers) implements Event {

    @Override
    public String toString() {
        return "KeyPressedEvent{" +
            "key=" + key +
            ", modifiers=" + modifiers +
            '}';
    }
}
