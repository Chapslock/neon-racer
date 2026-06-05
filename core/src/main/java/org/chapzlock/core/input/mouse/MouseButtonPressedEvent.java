package org.chapzlock.core.input.mouse;

import org.chapzlock.core.event.Event;

/**
 * Published when a mouse button is pressed.
 *
 * <p>Dispatched through the {@link org.chapzlock.core.event.EventBus} by the
 * {@link org.chapzlock.core.window.Window} GLFW callback. Screen coordinates use
 * top-left origin with Y increasing downward.
 *
 * @param button the mouse button code (see {@link org.chapzlock.core.input.MouseButton})
 * @param x cursor X position in screen pixels at time of press
 * @param y cursor Y position in screen pixels at time of press
 */
public record MouseButtonPressedEvent(int button, double x, double y) implements Event {

    @Override
    public String toString() {
        return "MouseButtonPressedEvent{button=" + button + ", x=" + x + ", y=" + y + '}';
    }
}
