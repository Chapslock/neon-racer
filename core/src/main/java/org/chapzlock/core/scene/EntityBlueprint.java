package org.chapzlock.core.scene;

import java.util.List;

import lombok.Getter;

@Getter
public class EntityBlueprint {
    private final String name;
    private final List<ComponentFactory> factories;

    public EntityBlueprint(String name, List<ComponentFactory> factories) {
        this.name = name;
        this.factories = List.copyOf(factories);
    }
}
