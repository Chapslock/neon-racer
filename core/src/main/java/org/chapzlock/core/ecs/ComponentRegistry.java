package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.chapzlock.core.component.Name;
import org.chapzlock.core.event.ComponentAddedEvent;
import org.chapzlock.core.event.ComponentRemovedEvent;
import org.chapzlock.core.event.EntityDestroyedEvent;
import org.chapzlock.core.event.Event;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.logging.Log;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Central registry for all components and entities.
 *
 * <p>Stores components in typed maps and provides fast archetype queries via bitset masks.
 * Query results are cached and invalidated on component mutations — {@link #view(Class[])}
 * is O(1) on stable frames.
 *
 * <p>Example:
 * <pre>{@code
 * int entity = Entity.create(registry);
 * registry.addComponent(entity, new Transform());
 * registry.addComponent(entity, new Mesh("model.obj"));
 *
 * for (EntityView e : registry.view(Transform.class, Mesh.class)) {
 *     Transform t = e.get(Transform.class);
 *     Mesh m = e.get(Mesh.class);
 *     // process entity
 * }
 * }</pre>
 */
public class ComponentRegistry {

    private final EventBus eventBus;

    private final Map<Class<? extends Component>, Integer> typeToId = new Object2IntOpenHashMap<>();
    private final List<Class<? extends Component>> idToType = new ArrayList<>();
    private final List<ComponentStore<?>> stores = new ArrayList<>();
    private final AtomicInteger nextTypeId = new AtomicInteger();
    /**
     * Tracks which components each entity has (bitset per entity)
     */
    private final Int2ObjectOpenHashMap<BitSet> entityComponentMasks = new Int2ObjectOpenHashMap<>();

    private final Map<BitSet, CachedQuery> queryCache = new HashMap<>();
    private final Map<String, Integer> nameIndex = new HashMap<>();

    private static class CachedQuery {
        final BitSet mask;
        List<Integer> entityIds;
        boolean valid;

        CachedQuery(BitSet mask) {
            this.mask = mask;
            this.entityIds = List.of();
            this.valid = false;
        }
    }

    public ComponentRegistry(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Publishes an event through the associated EventBus.
     */
    public void publishEvent(Event event) {
        eventBus.publish(event);
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
        eventBus.publish(new ComponentAddedEvent(entityId, component.getClass()));
        if (component instanceof Name nameComp) {
            String nameValue = nameComp.getValue();
            if (nameIndex.containsKey(nameValue)) {
                Log.debug("ComponentRegistry", "Name \"" + nameValue + "\" already in use by entity " +
                    nameIndex.get(nameValue) + " — overwriting with entity " + entityId);
            }
            nameIndex.put(nameValue, entityId);
        }
        invalidateCachesForType(typeId);
    }

    /**
     * Adds multiple components to an entity.
     * All components are stored first, then events are published.
     * This ensures sibling components are accessible during any event handler.
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
            eventBus.publish(new ComponentAddedEvent(entityId, component.getClass()));
        }
        for (T component : components) {
            if (component instanceof Name nameComp) {
                String nameValue = nameComp.getValue();
                if (nameIndex.containsKey(nameValue)) {
                    Log.debug("ComponentRegistry", "Name \"" + nameValue + "\" already in use by entity " +
                        nameIndex.get(nameValue) + " — overwriting with entity " + entityId);
                }
                nameIndex.put(nameValue, entityId);
            }
        }
        for (T component : components) {
            invalidateCachesForType(getOrRegisterTypeId(component.getClass()));
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
        eventBus.publish(new ComponentRemovedEvent(entityId, type));
        store.remove(entityId);
        if (type == Name.class) {
            nameIndex.values().removeIf(id -> id == entityId);
        }

        BitSet mask = entityComponentMasks.get(entityId);
        if (mask != null) {
            mask.clear(typeId);
            if (mask.isEmpty()) {
                entityComponentMasks.remove(entityId); // cleanup if no components left
            }
        }
        invalidateCachesForType(typeId);
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
            eventBus.publish(new ComponentRemovedEvent(entityId, type));
            stores.get(typeId).remove(entityId);
        }

        nameIndex.values().removeIf(id -> id == entityId);
        entityComponentMasks.remove(entityId);
        eventBus.publish(new EntityDestroyedEvent(entityId));
        invalidateAllCaches();
    }

    /**
     * Queries entities that have ALL required components.
     *
     * <p>Results are cached and invalidated on component mutations.
     * Safe to call every frame — O(1) when no entities changed.
     *
     * @param requiredComponents component types to query
     * @return list of entity views matching all required components
     */
    @SafeVarargs
    public final List<EntityView> view(Class<? extends Component>... requiredComponents) {
        if (requiredComponents.length == 0) {
            return Collections.emptyList();
        }

        final int[] requiredTypeIds = new int[requiredComponents.length];
        for (int i = 0; i < requiredComponents.length; i++) {
            Integer typeId = typeToId.get(requiredComponents[i]);
            if (typeId == null) {
                return Collections.emptyList();
            }
            requiredTypeIds[i] = typeId;
        }

        BitSet queryMask = new BitSet();
        for (int id : requiredTypeIds) {
            queryMask.set(id);
        }

        CachedQuery cached = queryCache.computeIfAbsent(queryMask, CachedQuery::new);
        if (!cached.valid) {
            List<Integer> ids = new ArrayList<>();
            for (Entry<Integer, BitSet> entry : entityComponentMasks.entrySet()) {
                BitSet entityMask = entry.getValue();
                boolean matches = true;
                for (int typeId : requiredTypeIds) {
                    if (!entityMask.get(typeId)) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    ids.add(entry.getKey());
                }
            }
            cached.entityIds = ids;
            cached.valid = true;
        }

        List<EntityView> results = new ArrayList<>(cached.entityIds.size());
        for (int entityId : cached.entityIds) {
            results.add(buildEntityView(entityId, requiredComponents, requiredTypeIds));
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
     * Returns the entity ID for a named entity, or -1 if no entity has that name.
     *
     * <p>Entities are indexed by name when a {@link Name} component is added to them.
     * This provides O(1) lookup by name instead of iterating all entities.
     *
     * @param name the name to look up
     * @return the entity ID, or -1 if not found
     * @see Name
     */
    public int getEntityByName(String name) {
        return nameIndex.getOrDefault(name, -1);
    }

    /**
     * Returns true if an entity with the given name exists in the registry.
     *
     * @param name the name to check
     * @return true if a named entity exists
     * @see Name
     */
    public boolean hasEntity(String name) {
        return nameIndex.containsKey(name);
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

    private void invalidateCachesForType(int typeId) {
        for (CachedQuery cached : queryCache.values()) {
            if (cached.mask.get(typeId)) {
                cached.valid = false;
            }
        }
    }

    private void invalidateAllCaches() {
        for (CachedQuery cached : queryCache.values()) {
            cached.valid = false;
        }
    }
}
