package org.chapzlock.core.ecs;

import org.chapzlock.core.event.EntityCreatedEvent;

/**
 * Factory for creating entity IDs.
 *
 * <p>Entities are plain integer IDs. Components are attached via {@link ComponentRegistry}.
 */
public class Entity {
    private Entity() {
    }

    /**
     * Creates a new entity ID and publishes {@link EntityCreatedEvent}.
     *
     * @param registry the component registry
     * @return a unique entity ID
     */
    public static int create(ComponentRegistry registry) {
        int id = EntityIdGenerator.nextId();
        registry.registerEntity(id);
        registry.publishEvent(new EntityCreatedEvent(id));
        return id;
    }
}
