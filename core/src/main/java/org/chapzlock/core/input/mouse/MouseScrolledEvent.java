package org.chapzlock.core.input.mouse;

import org.chapzlock.core.event.Event;

public record MouseScrolledEvent(double xOffset, double yOffset) implements Event {

    @Override
    public String toString() {
        return "MouseScrolledEvent{xOffset=" + xOffset + ", yOffset=" + yOffset + '}';
    }
}

