package org.chapzlock.core.event;

import org.chapzlock.core.ecs.Component;

/**
 * Published when a component is added to an entity.
 *
 * <p>Fired synchronously during {@link org.chapzlock.core.ecs.ComponentRegistry#addComponent}.
 * Systems can subscribe to this event to react to entity composition changes — for example,
 * PhysicsSystem listens for PhysicsBody additions to register entities into the simulation.
 *
 * <p><strong>Timing:</strong> During normal gameplay, this fires immediately. During scene
 * instantiation, events are deferred until the entity is fully assembled (all components added)
 * to prevent systems from seeing half-built entities.
 *
 * @param entityId the entity that received the component
 * @param componentType the component type that was added
 */
public record ComponentAddedEvent(int entityId, Class<? extends Component> componentType) implements Event {
}
