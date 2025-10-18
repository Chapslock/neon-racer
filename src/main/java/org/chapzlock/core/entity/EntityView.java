package org.chapzlock.core.entity;

import java.util.Map;

import org.chapzlock.core.component.Component;

import lombok.Getter;

/**
 * View of a single entity and all of its requested components
 */
public class EntityView {
    @Getter
    private final int id;
    private final Map<Class<? extends Component>, Component> components;

    private EntityView(int id, Map<Class<? extends Component>, Component> components) {
        this.id = id;
        this.components = components;
    }

    public static EntityView of(
        int id,
        Map<Class<? extends Component>, Component> components
    ) {
        return new EntityView(id, components);
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T get(Class<T> type) {
        return (T) components.get(type);
    }
}
