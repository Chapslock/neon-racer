package org.chapzlock.core.entity;

import java.util.Map;
import java.util.UUID;

import org.chapzlock.core.component.Component;

import lombok.Getter;

/**
 * View of a single entity and all of its requested components
 */
public class EntityView {
    @Getter
    private final UUID id;
    private final Map<Class<? extends Component>, Component> components;

    private EntityView(UUID id, Map<Class<? extends Component>, Component> components) {
        this.id = id;
        this.components = components;
    }

    public static EntityView of(
        UUID id,
        Map<Class<? extends Component>, Component> components
    ) {
        return new EntityView(id, components);
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T get(Class<T> type) {
        return (T) components.get(type);
    }
}
