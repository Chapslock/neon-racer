package org.chapzlock.core.event;

/**
 * Published when an entity is destroyed via {@link org.chapzlock.core.ecs.ComponentRegistry#destroyEntity(int)}.
 *
 * <p>Published after all {@link ComponentRemovedEvent}s for the entity's components.
 *
 * @param entityId the entity ID
 */
public record EntityDestroyedEvent(int entityId) implements Event {
}
