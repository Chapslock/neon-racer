package org.chapzlock.core.ecs;

import org.chapzlock.core.event.EntityCreatedEvent;

public class Entity {
    private Entity() {
    }

    public static int create(ComponentRegistry registry) {
        int id = EntityIdGenerator.nextId();
        registry.publishEvent(new EntityCreatedEvent(id));
        return id;
    }
}
