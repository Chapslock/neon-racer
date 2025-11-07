package org.chapzlock.core.input.mouse;

import org.chapzlock.core.event.Event;

public record MouseMovedEvent(double x, double y) implements Event {

    @Override
    public String toString() {
        return "MouseMovedEvent{x=" + x + ", y=" + y + '}';
    }
}

