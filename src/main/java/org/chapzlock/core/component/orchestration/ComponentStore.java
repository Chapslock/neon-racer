package org.chapzlock.core.component.orchestration;

import org.chapzlock.core.component.Component;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Component store keeps track of entity -> component relations for one Type of component
 *
 * @param <T> The component Type this store is meant for
 */
class ComponentStore<T extends Component> {
    private final Int2ObjectOpenHashMap<T> componentStore = new Int2ObjectOpenHashMap<>();

    void addComponentToStore(int entityId, T component) {
        componentStore.put(entityId, component);
    }

    T getComponentForEntity(int entityId) {
        return componentStore.get(entityId);
    }

    void remove(int entityId) {
        componentStore.remove(entityId);
    }

    Int2ObjectMap<T> getStore() {
        return componentStore;
    }

    boolean hasComponent(int entityId) {
        return componentStore.containsKey(entityId);
    }

    IntSet entityIds() {
        return componentStore.keySet();
    }
}
