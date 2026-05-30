package org.chapzlock.core.scene;

import java.util.List;

import lombok.Getter;

/**
 * Immutable blueprint for creating an entity.
 *
 * <p>Holds a name and a list of {@link ComponentFactory} lambdas that produce components.
 * Factories are invoked at scene instantiation time with access to {@link org.chapzlock.core.application.EngineContext}.
 */
@Getter
public class EntityBlueprint {
    private final String name;
    private final List<ComponentFactory> factories;

    public EntityBlueprint(String name, List<ComponentFactory> factories) {
        this.name = name;
        this.factories = List.copyOf(factories);
    }
}
