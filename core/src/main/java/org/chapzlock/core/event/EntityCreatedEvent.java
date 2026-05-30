package org.chapzlock.core.event;

/**
 * Published when an entity is created via {@link org.chapzlock.core.ecs.Entity#create(org.chapzlock.core.ecs.ComponentRegistry)}.
 *
 * @param entityId the entity ID
 */
public record EntityCreatedEvent(int entityId) implements Event {
}
