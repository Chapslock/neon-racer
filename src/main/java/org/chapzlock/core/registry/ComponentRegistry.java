package org.chapzlock.core.registry;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.chapzlock.core.component.Component;
import org.chapzlock.core.entity.EntityView;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Central registry for all the components and entities tied to them
 * Mainly used in Systems to execute logic
 */
public class ComponentRegistry {
    private final Map<Class<? extends Component>, Integer> typeToId = new HashMap<>();
    private final List<ComponentStore<?>> stores = new ArrayList<>();
    private final AtomicInteger nextTypeId = new AtomicInteger();

    // Track which components each entity has (bitset per entity)
    private final Int2ObjectOpenHashMap<BitSet> entityComponentMasks = new Int2ObjectOpenHashMap<>();

    /**
     * Assigns or retrieves an integer ID for a component type.
     */
    private int getOrRegisterTypeId(Class<? extends Component> type) {
        return typeToId.computeIfAbsent(type, t -> {
            int id = nextTypeId.getAndIncrement();
            // ensure stores grows
            while (stores.size() <= id) {
                stores.add(null);
            }
            stores.set(id, new ComponentStore<>());
            return id;
        });
    }

    /**
     * Adds a component to an entity.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> void addComponent(int entityId, T component) {
        int typeId = getOrRegisterTypeId(component.getClass());
        ComponentStore<T> store = (ComponentStore<T>) stores.get(typeId);
        store.put(entityId, component);

        BitSet mask = entityComponentMasks.computeIfAbsent(entityId, e -> new BitSet());
        mask.set(typeId);
    }

    /**
     * Retrieves a component from an entity.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(int entityId, Class<T> type) {
        Integer typeId = typeToId.get(type);
        if (typeId == null) {
            return null;
        }
        ComponentStore<T> store = (ComponentStore<T>) stores.get(typeId);
        return store.get(entityId);
    }

    /**
     * Removes a component from an entity.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> void removeComponent(int entityId, Class<T> type) {
        Integer typeId = typeToId.get(type);
        if (typeId == null) {
            return;
        }
        ComponentStore<T> store = (ComponentStore<T>) stores.get(typeId);
        store.remove(entityId);

        BitSet mask = entityComponentMasks.get(entityId);
        if (mask != null) {
            mask.clear(typeId);
            if (mask.isEmpty()) {
                entityComponentMasks.remove(entityId); // cleanup if no components left
            }
        }
    }

    /**
     * Queries entities that have ALL required components.
     */
    @SafeVarargs
    public final List<EntityView> view(Class<? extends Component>... requiredComponents) {
        if (requiredComponents.length == 0) {
            return Collections.emptyList();
        }

        // Resolve type ids once (and bail out early if any requested component type doesn't exist)
        final int[] requiredTypeIds = new int[requiredComponents.length];
        for (int i = 0; i < requiredComponents.length; i++) {
            Integer typeId = typeToId.get(requiredComponents[i]);
            if (typeId == null) {
                return Collections.emptyList(); // no such component registered
            }
            requiredTypeIds[i] = typeId;
        }

        List<EntityView> results = new ArrayList<>();
        // Iterate entity -> mask entries
        for (Entry<Integer, BitSet> entry : entityComponentMasks.entrySet()) {
            int entityId = entry.getKey();
            BitSet mask = entry.getValue();

            // Check all required bits are present in the entity mask
            boolean matches = true;
            for (int typeId : requiredTypeIds) {
                if (!mask.get(typeId)) { // BitSet.get is cheap and doesn't allocate
                    matches = false;
                    break;
                }
            }

            if (matches) {
                results.add(buildEntityView(entityId, requiredComponents, requiredTypeIds));
            }
        }
        return results;
    }

    /**
     * Helper that avoids re-looking-up the type ids for each component when building the view
     */
    private EntityView buildEntityView(int entityId, Class<? extends Component>[] requiredComponents, int[] requiredTypeIds) {
        Component[] comps = new Component[requiredComponents.length];
        for (int i = 0; i < requiredComponents.length; i++) {
            comps[i] = stores.get(requiredTypeIds[i]).get(entityId);
        }
        return EntityView.of(entityId, requiredComponents, comps);
    }
}
