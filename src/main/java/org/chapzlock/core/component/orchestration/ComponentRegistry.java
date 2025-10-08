package org.chapzlock.core.component.orchestration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.chapzlock.core.component.Component;
import org.chapzlock.core.entity.EntityView;

public class ComponentRegistry {
    private Map<Class<? extends Component>, EntityComponentStore<? extends Component>> componentTypeToStoreMap = new HashMap<>();

    // Register a new type of component store if it doesn't exist
    @SuppressWarnings("unchecked")
    private <T extends Component> EntityComponentStore<T> getStore(Class<T> type) {
        return (EntityComponentStore<T>) componentTypeToStoreMap.computeIfAbsent(type, t -> new EntityComponentStore<T>());
    }

    // Add a component to an entity
    public <T extends Component> void addComponent(UUID entityId, T component) {
        getStore((Class<T>) component.getClass()).add(entityId, component);
    }

    // Get a component from an entity
    public <T extends Component> T getComponent(UUID entityId, Class<T> type) {
        return getStore(type).get(entityId);
    }

    // Remove a component from an entity
    public <T extends Component> void removeComponent(UUID entityId, Class<T> type) {
        getStore(type).remove(entityId);
    }

    // Iterate all components of a given type
    public <T extends Component> Collection<Entry<UUID, T>> allComponents(Class<T> type) {
        return getStore(type).all();
    }

    // 🔑 New: Query entities that have all required components
    public List<EntityView> view(Class<? extends Component>... required) {
        List<EntityView> results = new ArrayList<>();

        // Start from the first store to reduce iteration cost
        if (required.length == 0) return results;

        EntityComponentStore<?> baseStore = componentTypeToStoreMap.get(required[0]);
        if (baseStore == null) return results;

        for (UUID id : baseStore.ids()) {
            Map<Class<? extends Component>, Component> comps = new HashMap<>();
            boolean valid = true;

            for (Class<? extends Component> type : required) {
                EntityComponentStore<?> store = componentTypeToStoreMap.get(type);
                if (store == null || !store.has(id)) {
                    valid = false;
                    break;
                }
                comps.put(type, store.get(id));
            }

            if (valid) {
                results.add(new EntityView(id, comps));
            }
        }
        return results;
    }
}
