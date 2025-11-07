package org.chapzlock.core.input.keyboard;

import org.chapzlock.core.event.Event;

/**
 * @param keyCode   GLFW key code
 * @param modifiers GLFW modifiers for the key (bitmap)
 */
public record KeyReleasedEvent(int keyCode, int modifiers) implements Event {

    @Override
    public String toString() {
        return "KeyReleasedEvent{" +
            "keyCode=" + keyCode +
            ", modifiers=" + modifiers +
            '}';
    }
}
