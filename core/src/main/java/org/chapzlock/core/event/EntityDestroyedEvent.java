package org.chapzlock.core.event;

/**
 * Published when an entity is destroyed via {@link org.chapzlock.core.ecs.ComponentRegistry#destroyEntity}.
 *
 * <p>Fired <em>before</em> components are removed — listeners can still query the entity's
 * components during handling. After all listeners return, components are stripped and the
 * entity ID becomes invalid.
 *
 * @param entityId the entity about to be destroyed
 */
public record EntityDestroyedEvent(int entityId) implements Event {
}
