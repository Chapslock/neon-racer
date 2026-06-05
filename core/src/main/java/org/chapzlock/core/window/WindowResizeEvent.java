package org.chapzlock.core.window;

import org.chapzlock.core.event.Event;

/**
 * Published when the window is resized by the user or the OS.
 *
 * <p>Systems that depend on window dimensions (e.g., CameraProjection for aspect ratio,
 * RenderSystem for viewport) subscribe to this event to update their state.
 *
 * @param width new window width in pixels
 * @param height new window height in pixels
 */
public record WindowResizeEvent(long windowId, long width, long height) implements Event {
}
