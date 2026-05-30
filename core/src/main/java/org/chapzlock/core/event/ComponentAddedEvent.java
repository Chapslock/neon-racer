package org.chapzlock.core.event;

import org.chapzlock.core.ecs.Component;

/**
 * Published when a component is added to an entity.
 *
 * @param entityId the entity ID
 * @param componentType the component type
 */
public record ComponentAddedEvent(int entityId, Class<? extends Component> componentType) implements Event {
}
