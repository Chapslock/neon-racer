package org.chapzlock.core.input.mouse;

import org.chapzlock.core.event.Event;

/**
 * Published when the mouse cursor moves.
 *
 * <p>Fires every frame the cursor position changes. When the cursor is locked
 * (e.g., first-person camera mode), coordinates represent accumulated delta movement
 * rather than absolute screen position.
 *
 * @param x cursor X position (or delta X when locked)
 * @param y cursor Y position (or delta Y when locked)
 */
public record MouseMovedEvent(double x, double y) implements Event {

    @Override
    public String toString() {
        return "MouseMovedEvent{x=" + x + ", y=" + y + '}';
    }
}

