package org.chapzlock.core.event;

/**
 * Published when a new entity is created via {@link org.chapzlock.core.ecs.Entity#create}.
 *
 * <p>At the time this event fires, the entity ID exists in the registry but has no
 * components yet. Subscribe to {@link ComponentAddedEvent} instead if you need to react
 * to specific component compositions.
 *
 * @param entityId the newly created entity ID
 */
public record EntityCreatedEvent(int entityId) implements Event {
}
