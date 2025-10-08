package org.chapzlock.core.component.orchestration;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.chapzlock.core.component.Component;

/**
 * Component store keeps track of entity -> component relations for one Type of component
 *
 * @param <T> The component Type this store is meant for
 */
class ComponentStore<T extends Component> {
    private final Map<UUID, T> entityToComponentMap = new HashMap<>();

    void addComponentToStore(UUID entityId, T component) {
        entityToComponentMap.put(entityId, component);
    }

    T getComponentForEntity(UUID entityId) {
        return entityToComponentMap.get(entityId);
    }

    void remove(UUID entityId) {
        entityToComponentMap.remove(entityId);
    }

    Set<Entry<UUID, T>> getAllEntries() {
        return entityToComponentMap.entrySet();
    }

    boolean hasComponent(UUID entityId) {
        return entityToComponentMap.containsKey(entityId);
    }

    Set<UUID> entityIds() {
        return entityToComponentMap.keySet();
    }
}
