package org.chapzlock.core.component.orchestration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.chapzlock.core.component.Component;

/**
 * Component store keeps track of entity -> component relations for one Type of component
 *
 * @param <T> The component Type this store is meant for
 */
class ComponentStore<T extends Component> {
    private final Map<UUID, T> componentStore = new HashMap<>();

    void addComponentToStore(UUID entityId, T component) {
        componentStore.put(entityId, component);
    }

    T getComponentForEntity(UUID entityId) {
        return componentStore.get(entityId);
    }

    void remove(UUID entityId) {
        componentStore.remove(entityId);
    }

    Map<UUID, T> getStore() {
        return componentStore;
    }

    boolean hasComponent(UUID entityId) {
        return componentStore.containsKey(entityId);
    }

    Set<UUID> entityIds() {
        return componentStore.keySet();
    }
}
