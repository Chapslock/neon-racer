package org.chapzlock.core.event;

public record EntityDestroyedEvent(int entityId) implements Event {
}
