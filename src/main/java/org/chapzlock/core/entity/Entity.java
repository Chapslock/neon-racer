package org.chapzlock.core.entity;

import java.util.UUID;

public class Entity {
    private final UUID id;

    private Entity() {
        id = UUID.randomUUID();
    }

    private UUID getId() {
        return id;
    }

    public static UUID create() {
        return new Entity().getId();
    }
}
