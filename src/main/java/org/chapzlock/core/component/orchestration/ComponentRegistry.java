package org.chapzlock.core.component.orchestration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.component.Component;
import org.chapzlock.core.entity.EntityView;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Central registry for all the components and entities tied to them
 * Mainly used in Systems to execute logic
 */
public class ComponentRegistry {
    private final Map<Class<? extends Component>, ComponentStore<? extends Component>> registry = new HashMap<>();

    /**
     * Tries to find a componentStore of a specific type.
     * If not such store exists, then creates an entry for it in the registry
     *
     * @param type Type of component used for search
     * @param <T> implements Component
     * @return always returns the store for the specified type
     */
    @SuppressWarnings("unchecked")
    private <T extends Component> ComponentStore<T> getStoreOfTypeOrRegisterNew(Class<T> type) {
        return (ComponentStore<T>) registry.computeIfAbsent(type, t -> new ComponentStore<T>());
    }

    /**
     * Add a component to an entity.
     *
     * @param entityId id of the entity
     * @param component The component to add
     * @param <T> implements Component
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> void addComponent(int entityId, T component) {
        getStoreOfTypeOrRegisterNew((Class<T>) component.getClass()).addComponentToStore(entityId, component);
    }

    /**
     * Get a component from an entity
     *
     * @param entityId id of the entity
     * @param type     the type of component to search for
     * @param <T> implements Component
     * @return Component if it exists on an entity otherwise null
     */
    public <T extends Component> T getComponent(int entityId, Class<T> type) {
        return getStoreOfTypeOrRegisterNew(type).getComponentForEntity(entityId);
    }

    /**
     * Remove a component from an entity
     * @param entityId entity from which to remove the component
     * @param type the component to remove
     * @param <T> implements Component
     */
    public <T extends Component> void removeComponent(int entityId, Class<T> type) {
        getStoreOfTypeOrRegisterNew(type).remove(entityId);
    }

    /**
     * Finds all entities that have the component attached
     * @param type Type of the component to search for
     * @return Map of entity Ids and components
     * @param <T> implements Component
     */
    public <T extends Component> Int2ObjectMap<T> getStoreForType(Class<T> type) {
        return getStoreOfTypeOrRegisterNew(type).getStore();
    }

    /**
     * Queries entities that have all the required components attached
     *
     * @param requiredComponents varargs list of required component classes
     * @return List of entity views, which have all the required components. Returns an empty list if no matches are found
     */
    @SafeVarargs
    public final List<EntityView> view(Class<? extends Component>... requiredComponents) {
        if (requiredComponents.length == 0) {
            return Collections.emptyList();
        }

        List<IntSet> entitySets = new ArrayList<>(requiredComponents.length);

        for (Class<? extends Component> comp : requiredComponents) {
            ComponentStore<?> store = registry.get(comp);
            if (store == null) {
                return Collections.emptyList(); // no entities have this component
            }
            entitySets.add(store.entityIds());
        }

        // Sort stores by size: smallest first for faster intersection
        entitySets.sort(Comparator.comparingInt(IntSet::size));

        // Intersect all entity sets
        IntSet intersection = new IntOpenHashSet(entitySets.get(0));
        for (int i = 1; i < entitySets.size(); i++) {
            intersection.retainAll(entitySets.get(i));
            if (intersection.isEmpty()) {
                return Collections.emptyList();
            }
        }

        // Build entity views
        List<EntityView> results = new ArrayList<>(intersection.size());
        for (int entityId : intersection) {
            Map<Class<? extends Component>, Component> comps = new HashMap<>(requiredComponents.length);
            for (Class<? extends Component> compType : requiredComponents) {
                comps.put(compType, registry.get(compType).getComponentForEntity(entityId));
            }
            results.add(EntityView.of(entityId, comps));
        }
        return results;
    }
}
