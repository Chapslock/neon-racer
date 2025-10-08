package org.chapzlock.core.component.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.chapzlock.core.component.Component;
import org.chapzlock.core.entity.EntityView;

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
    public <T extends Component> void addComponent(UUID entityId, T component) {
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
    public <T extends Component> T getComponent(UUID entityId, Class<T> type) {
        return getStoreOfTypeOrRegisterNew(type).getComponentForEntity(entityId);
    }

    /**
     * Remove a component from an entity
     * @param entityId entity from which to remove the component
     * @param type the component to remove
     * @param <T> implements Component
     */
    public <T extends Component> void removeComponent(UUID entityId, Class<T> type) {
        getStoreOfTypeOrRegisterNew(type).remove(entityId);
    }

    /**
     * Finds all entities that have the component attached
     * @param type Type of the component to search for
     * @return Map of entity Ids and components
     * @param <T> implements Component
     */
    public <T extends Component> Map<UUID, T> getStoreForType(Class<T> type) {
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
        List<EntityView> results = new ArrayList<>();

        if (requiredComponents.length == 0) {
            return results;
        }

        // Find the store linked to the first component
        ComponentStore<?> baseStore = registry.get(requiredComponents[0]);
        if (baseStore == null) {
            System.out.println("Could not find any entities with component: " + requiredComponents[0]);
            return results;
        }

        //Loop through all the entities in the first store that was found
        for (UUID entityId : baseStore.entityIds()) {
            Map<Class<? extends Component>, Component> components = new HashMap<>();
            boolean hasAllRequiredComponents = true;

            //Make sure the entity has all other required components as well
            for (Class<? extends Component> componentType : requiredComponents) {
                ComponentStore<?> store = registry.get(componentType);
                if (store == null || !store.hasComponent(entityId)) {
                    hasAllRequiredComponents = false;
                    break;
                }
                components.put(componentType, store.getComponentForEntity(entityId));
            }

            if (hasAllRequiredComponents) {
                results.add(EntityView.of(entityId, components));
            }
        }
        return results;
    }
}
