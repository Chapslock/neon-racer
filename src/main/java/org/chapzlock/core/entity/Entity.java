package org.chapzlock.core.entity;

public class Entity {
    private final int id;

    private Entity() {
        id = EntityIdGenerator.nextId();
    }

    private int getId() {
        return id;
    }

    public static int create() {
        return new Entity().getId();
    }
}
