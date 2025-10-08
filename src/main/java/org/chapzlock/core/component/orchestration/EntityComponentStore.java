package org.chapzlock.core.component.orchestration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.chapzlock.core.component.Component;

public class EntityComponentStore<T extends Component> {
    private final Map<UUID, T> entityIdToComponentMap = new HashMap<>();

    public void add(UUID entityId, T component) {
        entityIdToComponentMap.put(entityId, component);
    }

    public T get(UUID entityId) {
        return entityIdToComponentMap.get(entityId);
    }

    public void remove(UUID entityId) {
        entityIdToComponentMap.remove(entityId);
    }

    public Collection<Entry<UUID, T>> all() {
        return entityIdToComponentMap.entrySet();
    }

    public boolean has(UUID entityId) {
        return entityIdToComponentMap.containsKey(entityId);
    }

    public Set<UUID> ids() {
        return entityIdToComponentMap.keySet();
    }
}
