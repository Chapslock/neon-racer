package org.chapzlock.core.registry;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.chapzlock.core.application.Component;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.event.ComponentAddedEvent;
import org.chapzlock.core.event.ComponentRemovedEvent;
import org.chapzlock.core.event.EntityDestroyedEvent;
import org.chapzlock.core.event.EventBus;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Central registry for all the components and entities tied to them
 * Mainly used in Systems to execute logic
 * Implemented as a singleton because it carries the global state of entities
 */
public class ComponentRegistry {
    private static ComponentRegistry instance;

    private final Map<Class<? extends Component>, Integer> typeToId = new Object2IntOpenHashMap<>();
    private final List<Class<? extends Component>> idToType = new ArrayList<>();
    private final List<ComponentStore<?>> stores = new ArrayList<>();
    private final AtomicInteger nextTypeId = new AtomicInteger();
    /**
     * Tracks which components each entity has (bitset per entity)
     */
    private final Int2ObjectOpenHashMap<BitSet> entityComponentMasks = new Int2ObjectOpenHashMap<>();

    private ComponentRegistry() {
    }

    public static ComponentRegistry instance() {
        if (instance == null) {
            instance = new ComponentRegistry();
        }
        return instance;
    }

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
            while (idToType.size() <= id) {
                idToType.add(null);
            }
            idToType.set(id, t);
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
        EventBus.instance().publish(new ComponentAddedEvent(entityId, component.getClass()));
    }

    /**
     * Adds multiple components to an entity.
     * All components are stored first, then events are published.
     * This ensures sibling components are accessible during any event handler.
     *
     * @param entityId
     * @param components
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> void addComponents(int entityId, T... components) {
        for (T component : components) {
            int typeId = getOrRegisterTypeId(component.getClass());
            ComponentStore<T> store = (ComponentStore<T>) stores.get(typeId);
            store.put(entityId, component);

            BitSet mask = entityComponentMasks.computeIfAbsent(entityId, e -> new BitSet());
            mask.set(typeId);
        }
        for (T component : components) {
            EventBus.instance().publish(new ComponentAddedEvent(entityId, component.getClass()));
        }
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
        EventBus.instance().publish(new ComponentRemovedEvent(entityId, type));
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
     * Destroys an entity by removing all its components and publishing lifecycle events.
     * ComponentRemovedEvent is published for each component (while still accessible),
     * then EntityDestroyedEvent is published last.
     */
    public void destroyEntity(int entityId) {
        BitSet mask = entityComponentMasks.get(entityId);
        if (mask == null) return;

        for (int typeId = mask.nextSetBit(0); typeId >= 0; typeId = mask.nextSetBit(typeId + 1)) {
            Class<? extends Component> type = idToType.get(typeId);
            EventBus.instance().publish(new ComponentRemovedEvent(entityId, type));
            stores.get(typeId).remove(entityId);
        }

        entityComponentMasks.remove(entityId);
        EventBus.instance().publish(new EntityDestroyedEvent(entityId));
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
     * Returns the first entity matching ALL required components, or empty if none match.
     * Useful for singleton-style entities (e.g. Camera, Sky, PointLight).
     */
    @SafeVarargs
    public final Optional<EntityView> viewFirst(Class<? extends Component>... requiredComponents) {
        List<EntityView> results = view(requiredComponents);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
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
