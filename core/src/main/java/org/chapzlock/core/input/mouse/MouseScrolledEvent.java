package org.chapzlock.core.input.mouse;

import org.chapzlock.core.event.Event;

/**
 * Published when the mouse scroll wheel moves.
 *
 * <p>Most mice only produce Y offset (vertical scroll). Horizontal scrolling
 * (X offset) is available on trackpads and some specialized mice.
 *
 * @param xOffset horizontal scroll amount (positive = right)
 * @param yOffset vertical scroll amount (positive = up/away from user)
 */
public record MouseScrolledEvent(double xOffset, double yOffset) implements Event {

    @Override
    public String toString() {
        return "MouseScrolledEvent{xOffset=" + xOffset + ", yOffset=" + yOffset + '}';
    }
}

