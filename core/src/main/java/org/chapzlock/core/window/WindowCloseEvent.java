package org.chapzlock.core.window;

import org.chapzlock.core.event.Event;

public record WindowCloseEvent(long windowId) implements Event {
}
