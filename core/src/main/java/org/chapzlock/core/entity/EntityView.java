package org.chapzlock.core.entity;

import org.chapzlock.core.application.Component;

import lombok.Getter;

/**
 * Lightweight view of an entity and its components
 */
public class EntityView {
    @Getter
    private final int id;
    private final Class<? extends Component>[] types;
    private final Component[] components;

    private EntityView(int id, Class<? extends Component>[] types, Component[] components) {
        this.id = id;
        this.types = types;
        this.components = components;
    }

    public static EntityView of(int id, Class<? extends Component>[] types, Component[] components) {
        return new EntityView(id, types, components);
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T get(Class<T> type) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == type) {
                return (T) components[i];
            }
        }
        return null;
    }
}
