package org.chapzlock.core.input.mouse;

import org.chapzlock.core.event.Event;

public record MouseButtonReleasedEvent(int button, double x, double y) implements Event {

    @Override
    public String toString() {
        return "MouseButtonReleasedEvent{button=" + button + ", x=" + x + ", y=" + y + '}';
    }
}
