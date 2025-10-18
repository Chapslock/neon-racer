package org.chapzlock.core.component.orchestration;

import org.chapzlock.core.component.Component;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Simple store for components of one type
 */
class ComponentStore<T extends Component> {
    private final Int2ObjectOpenHashMap<T> data = new Int2ObjectOpenHashMap<>();

    void put(int entityId, T comp) {
        data.put(entityId, comp);
    }

    T get(int entityId) {
        return data.get(entityId);
    }

    void remove(int entityId) {
        data.remove(entityId);
    }
}
