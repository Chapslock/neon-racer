package org.chapzlock.core.window;

import org.chapzlock.core.event.Event;

/**
 * Published when the user requests the window to close (clicking X or pressing Alt+F4).
 *
 * <p>The engine's main loop checks for this event to initiate graceful shutdown.
 * Subscribing to this event allows game code to intercept the close (e.g., "unsaved progress"
 * confirmation dialog) by consuming the event.
 */
public record WindowCloseEvent(long windowId) implements Event {
}
