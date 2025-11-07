package org.chapzlock.core.window;

import org.chapzlock.core.event.Event;

public record WindowResizeEvent(long windowId, long width, long height) implements Event {
}
