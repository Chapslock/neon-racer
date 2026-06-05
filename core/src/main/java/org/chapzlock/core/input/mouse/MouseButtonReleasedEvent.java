package org.chapzlock.core.input.mouse;

import org.chapzlock.core.event.Event;

/**
 * Published when a mouse button is released.
 *
 * <p>Dispatched through the {@link org.chapzlock.core.event.EventBus} by the
 * {@link org.chapzlock.core.window.Window} GLFW callback.
 *
 * @param button the mouse button code (see {@link org.chapzlock.core.input.MouseButton})
 * @param x cursor X position in screen pixels at time of release
 * @param y cursor Y position in screen pixels at time of release
 */
public record MouseButtonReleasedEvent(int button, double x, double y) implements Event {

    @Override
    public String toString() {
        return "MouseButtonReleasedEvent{button=" + button + ", x=" + x + ", y=" + y + '}';
    }
}
