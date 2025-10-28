package org.chapzlock.core.registry;

import org.chapzlock.core.application.Component;

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
