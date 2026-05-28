package org.chapzlock.core.ecs;

import org.chapzlock.core.event.EntityCreatedEvent;
import org.chapzlock.core.event.EventBus;

public class Entity {
    private Entity() {
    }

    public static int create() {
        int id = EntityIdGenerator.nextId();
        EventBus.instance().publish(new EntityCreatedEvent(id));
        return id;
    }
}
