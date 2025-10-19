package org.chapzlock.core.entity;

public class Entity {
    private Entity() {
    }

    public static int create() {
        return EntityIdGenerator.nextId();
    }
}
