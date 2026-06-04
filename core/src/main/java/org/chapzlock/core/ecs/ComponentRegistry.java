package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.BitSet;
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
 * Query results are cached and invalidated on component mutations — queries are O(1) on
 * stable frames.
 *
 * <p>Example:
 * <pre>{@code
 * int entity = Entity.create(registry);
 * registry.addComponent(entity, new Transform());
 * registry.addComponent(entity, new Mesh("model.obj"));
 *
 * registry.forEach(Transform.class, Mesh.class, (entityId, transform, mesh) -> {
 *     // process entity
 * });
 * }</pre>
 */
public class ComponentRegistry {

    private static final Log log = Log.create(ComponentRegistry.class);

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
    private final List<ComponentRule> componentRules = new ArrayList<>();

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
     * Returns whether an entity exists in the registry.
     *
     * <p>An entity "exists" if it has been created via {@link Entity#create(ComponentRegistry)}
     * and has not been destroyed via {@link #destroyEntity(int)}. After creation, an empty
     * component mask is registered — this is what makes the entity "exist" even before any
     * components are added.
     *
     * <p><strong>When to use:</strong> Check this before operating on an entity ID that may
     * be stale (e.g., stored from a previous frame or scene). Avoids silently adding components
     * to destroyed entities.
     *
     * @param entityId the entity ID to check
     * @return true if the entity has a component mask in the registry
     */
    public boolean entityExists(int entityId) {
        return entityComponentMasks.containsKey(entityId);
    }

    /**
     * Registers an entity ID in the registry with an empty component mask.
     *
     * <p>Called by {@link Entity#create(ComponentRegistry)} to ensure the entity is recognized
     * as "existing" before any components are added. Without this, the guard in
     * {@link #addComponent(int, Component)} would reject the first component.
     *
     * @param entityId the newly created entity ID
     */
    public void registerEntity(int entityId) {
        entityComponentMasks.computeIfAbsent(entityId, e -> new java.util.BitSet());
    }

    /**
     * Adds a component to an entity.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> void addComponent(int entityId, T component) {
        if (!entityExists(entityId)) {
            log.warn("Attempted to add %s to non-existent entity %d. Ignoring. Ensure the entity was created via Entity.create() and has not been destroyed.",
                component.getClass().getSimpleName(), entityId);
            return;
        }
        int typeId = getOrRegisterTypeId(component.getClass());
        ComponentStore<T> store = (ComponentStore<T>) stores.get(typeId);
        store.put(entityId, component);

        BitSet mask = entityComponentMasks.computeIfAbsent(entityId, e -> new BitSet());
        mask.set(typeId);
        eventBus.publish(new ComponentAddedEvent(entityId, component.getClass()));
        if (component instanceof Name nameComp) {
            String nameValue = nameComp.getValue();
            if (nameIndex.containsKey(nameValue)) {
                log.debug("Name \"%s\" already in use by entity %d — overwriting with entity %d",
                    nameValue, nameIndex.get(nameValue), entityId);
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
        if (!entityExists(entityId)) {
            log.warn("Attempted to add components to non-existent entity %d. Ignoring. Ensure the entity was created via Entity.create() and has not been destroyed.",
                entityId);
            return;
        }
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
                    log.debug("Name \"%s\" already in use by entity %d — overwriting with entity %d",
                        nameValue, nameIndex.get(nameValue), entityId);
                }
                nameIndex.put(nameValue, entityId);
            }
        }
        for (T component : components) {
            invalidateCachesForType(getOrRegisterTypeId(component.getClass()));
        }
    }

    /**
     * Adds multiple components to an entity without firing any events.
     *
     * <p>Use this for atomic entity assembly where all components must be in place before
     * systems are notified. After calling this, fire events explicitly (e.g. via
     * {@link #publishEvent(Event)}) once the entity is fully assembled.
     *
     * <p>Components are stored, bitset masks are updated, and query caches are invalidated,
     * but no {@link ComponentAddedEvent} is published. This guarantees that systems only see
     * fully-assembled entities when events are eventually fired.
     *
     * @param entityId   the entity to add components to
     * @param components the components to add
     */
    @SuppressWarnings("unchecked")
    public void addComponentsSilent(int entityId, List<? extends Component> components) {
        if (!entityExists(entityId)) {
            log.warn("Attempted to silently add components to non-existent entity %d. Ignoring. Ensure the entity was created via Entity.create() and has not been destroyed.",
                entityId);
            return;
        }
        BitSet mask = entityComponentMasks.computeIfAbsent(entityId, e -> new BitSet());
        for (Component component : components) {
            int typeId = getOrRegisterTypeId(component.getClass());
            ComponentStore<Component> store = (ComponentStore<Component>) stores.get(typeId);
            store.put(entityId, component);
            mask.set(typeId);
            if (component instanceof Name nameComp) {
                String nameValue = nameComp.getValue();
                if (nameIndex.containsKey(nameValue)) {
                    log.debug("Name \"%s\" already in use by entity %d — overwriting with entity %d",
                        nameValue, nameIndex.get(nameValue), entityId);
                }
                nameIndex.put(nameValue, entityId);
            }
        }
        for (Component component : components) {
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
        if (type == Name.class) {
            Name nameComp = (Name) store.get(entityId);
            if (nameComp != null) {
                nameIndex.remove(nameComp.getValue());
            }
        }
        eventBus.publish(new ComponentRemovedEvent(entityId, type));
        store.remove(entityId);

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
     * Registers a component dependency rule for validation during entity creation.
     *
     * @param rule the dependency rule to register
     * @see ComponentRule
     */
    public void addRule(ComponentRule rule) {
        componentRules.add(rule);
    }

    /**
     * Returns all registered component rules.
     *
     * @return unmodifiable view of the rules list
     */
    public List<ComponentRule> getRules() {
        return java.util.Collections.unmodifiableList(componentRules);
    }

    /**
     * Returns the single instance of a component type, or empty if no entity has it.
     *
     * <p>If multiple entities have this component, returns the first and logs a debug
     * warning. Use {@link #forEach(Class, ComponentConsumer1)} if multiple are expected.
     *
     * @param <T> the component type
     * @param type the component class to look up
     * @return the component instance wrapped in Optional, or empty
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getSingleton(Class<T> type) {
        Integer typeId = typeToId.get(type);
        if (typeId == null) {
            return Optional.empty();
        }

        BitSet queryMask = new BitSet();
        queryMask.set(typeId);

        CachedQuery cached = queryCache.computeIfAbsent(queryMask, CachedQuery::new);
        if (!cached.valid) {
            List<Integer> ids = new ArrayList<>();
            for (Entry<Integer, BitSet> entry : entityComponentMasks.entrySet()) {
                if (entry.getValue().get(typeId)) {
                    ids.add(entry.getKey());
                }
            }
            cached.entityIds = ids;
            cached.valid = true;
        }

        if (cached.entityIds.isEmpty()) {
            return Optional.empty();
        }

        if (cached.entityIds.size() > 1) {
            log.debug("getSingleton(%s) found %d entities — returning first. Use forEach() if multiple are intentional.",
                type.getSimpleName(), cached.entityIds.size());
        }

        ComponentStore<T> store = (ComponentStore<T>) stores.get(typeId);
        return Optional.ofNullable(store.get(cached.entityIds.getFirst()));
    }

    /**
     * Resolves a cached query for the given component type IDs.
     *
     * <p>Creates a bitset mask from the type IDs, looks up or creates a cached query,
     * and recomputes entity matches if the cache is invalid. Returns the cached list
     * of matching entity IDs.
     *
     * @param typeIds the component type IDs to query for (all must be present on matching entities)
     * @return immutable list of entity IDs matching all type IDs
     */
    private List<Integer> resolveQuery(int... typeIds) {
        BitSet queryMask = new BitSet();
        for (int id : typeIds) {
            queryMask.set(id);
        }

        CachedQuery cached = queryCache.computeIfAbsent(queryMask, CachedQuery::new);
        if (!cached.valid) {
            List<Integer> ids = new ArrayList<>();
            for (Entry<Integer, BitSet> entry : entityComponentMasks.entrySet()) {
                BitSet mask = entry.getValue();
                boolean matches = true;
                for (int typeId : typeIds) {
                    if (!mask.get(typeId)) {
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
        return cached.entityIds;
    }

    /**
     * Iterates all entities with the specified component, passing typed references to the consumer.
     *
     * <p>Zero allocations on stable frames — uses the same cached query mechanism as other queries.
     *
     * <p>Example:
     * <pre>{@code
     * registry.forEach(Velocity.class, (entityId, vel) -> {
     *     // process velocity for each entity
     * });
     * }</pre>
     *
     * @param <A> the component type
     * @param type the component class to query
     * @param consumer callback receiving entity ID and the component instance
     */
    @SuppressWarnings("unchecked")
    public <A extends Component> void forEach(Class<A> type, ComponentConsumer1<A> consumer) {
        Integer typeIdA = typeToId.get(type);
        if (typeIdA == null) return;

        List<Integer> entities = resolveQuery(typeIdA);
        ComponentStore<A> storeA = (ComponentStore<A>) stores.get(typeIdA);
        for (int entityId : entities) {
            consumer.accept(entityId, storeA.get(entityId));
        }
    }

    /**
     * Iterates all entities with both specified components, passing typed references to the consumer.
     *
     * <p>Zero allocations on stable frames — uses the same cached query mechanism as other queries.
     *
     * <p>Example:
     * <pre>{@code
     * registry.forEach(Transform.class, Velocity.class, (entityId, transform, vel) -> {
     *     transform.translate(vel.dx() * delta, vel.dy() * delta, 0);
     * });
     * }</pre>
     *
     * @param <A> first component type
     * @param <B> second component type
     * @param typeA first component class to query
     * @param typeB second component class to query
     * @param consumer callback receiving entity ID and both component instances
     */
    @SuppressWarnings("unchecked")
    public <A extends Component, B extends Component> void forEach(
            Class<A> typeA, Class<B> typeB, ComponentConsumer2<A, B> consumer) {
        Integer typeIdA = typeToId.get(typeA);
        Integer typeIdB = typeToId.get(typeB);
        if (typeIdA == null || typeIdB == null) return;

        List<Integer> entities = resolveQuery(typeIdA, typeIdB);
        ComponentStore<A> storeA = (ComponentStore<A>) stores.get(typeIdA);
        ComponentStore<B> storeB = (ComponentStore<B>) stores.get(typeIdB);
        for (int entityId : entities) {
            consumer.accept(entityId, storeA.get(entityId), storeB.get(entityId));
        }
    }

    /**
     * Iterates all entities with all three specified components, passing typed references to the consumer.
     *
     * <p>Zero allocations on stable frames — uses the same cached query mechanism as other queries.
     *
     * <p>Example:
     * <pre>{@code
     * registry.forEach(Transform.class, Mesh.class, Material.class, (entityId, t, mesh, mat) -> {
     *     // render entity with all three components
     * });
     * }</pre>
     *
     * @param <A> first component type
     * @param <B> second component type
     * @param <C> third component type
     * @param typeA first component class to query
     * @param typeB second component class to query
     * @param typeC third component class to query
     * @param consumer callback receiving entity ID and all three component instances
     */
    @SuppressWarnings("unchecked")
    public <A extends Component, B extends Component, C extends Component> void forEach(
            Class<A> typeA, Class<B> typeB, Class<C> typeC, ComponentConsumer3<A, B, C> consumer) {
        Integer typeIdA = typeToId.get(typeA);
        Integer typeIdB = typeToId.get(typeB);
        Integer typeIdC = typeToId.get(typeC);
        if (typeIdA == null || typeIdB == null || typeIdC == null) return;

        List<Integer> entities = resolveQuery(typeIdA, typeIdB, typeIdC);
        ComponentStore<A> storeA = (ComponentStore<A>) stores.get(typeIdA);
        ComponentStore<B> storeB = (ComponentStore<B>) stores.get(typeIdB);
        ComponentStore<C> storeC = (ComponentStore<C>) stores.get(typeIdC);
        for (int entityId : entities) {
            consumer.accept(entityId, storeA.get(entityId), storeB.get(entityId), storeC.get(entityId));
        }
    }

    /**
     * Iterates all entities with all four specified components, passing typed references to the consumer.
     *
     * <p>Zero allocations on stable frames — uses the same cached query mechanism as other queries.
     *
     * <p>Example:
     * <pre>{@code
     * registry.forEach(Transform.class, Mesh.class, Material.class, OpaqueLayer.class,
     *     (entityId, transform, mesh, material, layer) -> {
     *         // render opaque entity
     *     });
     * }</pre>
     *
     * @param <A> first component type
     * @param <B> second component type
     * @param <C> third component type
     * @param <D> fourth component type
     * @param typeA first component class to query
     * @param typeB second component class to query
     * @param typeC third component class to query
     * @param typeD fourth component class to query
     * @param consumer callback receiving entity ID and all four component instances
     */
    @SuppressWarnings("unchecked")
    public <A extends Component, B extends Component, C extends Component, D extends Component> void forEach(
            Class<A> typeA, Class<B> typeB, Class<C> typeC, Class<D> typeD,
            ComponentConsumer4<A, B, C, D> consumer) {
        Integer typeIdA = typeToId.get(typeA);
        Integer typeIdB = typeToId.get(typeB);
        Integer typeIdC = typeToId.get(typeC);
        Integer typeIdD = typeToId.get(typeD);
        if (typeIdA == null || typeIdB == null || typeIdC == null || typeIdD == null) return;

        List<Integer> entities = resolveQuery(typeIdA, typeIdB, typeIdC, typeIdD);
        ComponentStore<A> storeA = (ComponentStore<A>) stores.get(typeIdA);
        ComponentStore<B> storeB = (ComponentStore<B>) stores.get(typeIdB);
        ComponentStore<C> storeC = (ComponentStore<C>) stores.get(typeIdC);
        ComponentStore<D> storeD = (ComponentStore<D>) stores.get(typeIdD);
        for (int entityId : entities) {
            consumer.accept(entityId, storeA.get(entityId), storeB.get(entityId),
                storeC.get(entityId), storeD.get(entityId));
        }
    }

    /**
     * Returns the number of entities matching ALL required component types.
     *
     * <p>Uses the same cached query mechanism as forEach — O(1) on stable frames,
     * zero allocations after the first call. Prefer this over iterating when
     * you only need the count and don't need to process the entities.
     *
     * <p>Example:
     * <pre>{@code
     * int enemies = registry.count(Enemy.class, Transform.class);
     * if (enemies == 0) {
     *     // spawn next wave
     * }
     * }</pre>
     *
     * @param requiredComponents component types that entities must have (all must be present)
     * @return number of matching entities, or 0 if no entities match or types are unregistered
     */
    @SafeVarargs
    public final int count(Class<? extends Component>... requiredComponents) {
        if (requiredComponents.length == 0) {
            return 0;
        }

        final int[] requiredTypeIds = new int[requiredComponents.length];
        for (int i = 0; i < requiredComponents.length; i++) {
            Integer typeId = typeToId.get(requiredComponents[i]);
            if (typeId == null) {
                return 0;
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

        return cached.entityIds.size();
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
