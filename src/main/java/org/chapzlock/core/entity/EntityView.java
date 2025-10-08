package org.chapzlock.core.entity;

import java.util.Map;
import java.util.UUID;

import org.chapzlock.core.component.Component;

public class EntityView {
    private final UUID id;
    private final Map<Class<? extends Component>, Component> components;

    public EntityView(UUID id, Map<Class<? extends Component>, Component> components) {
        this.id = id;
        this.components = components;
    }

    public UUID getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T get(Class<T> type) {
        return (T) components.get(type);
    }
}
