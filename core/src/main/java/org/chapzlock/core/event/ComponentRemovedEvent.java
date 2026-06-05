package org.chapzlock.core.event;

import org.chapzlock.core.ecs.Component;

/**
 * Published when a component is removed from an entity.
 *
 * <p>Fired synchronously during component removal. Systems should use this to clean up
 * any external state they maintain for the entity — for example, PhysicsSystem removes
 * the rigid body from the simulation when PhysicsBody is removed.
 *
 * @param entityId the entity that lost the component
 * @param componentType the component type that was removed
 */
public record ComponentRemovedEvent(int entityId, Class<? extends Component> componentType) implements Event {
}
