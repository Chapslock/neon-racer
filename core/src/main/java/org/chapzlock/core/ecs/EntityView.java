package org.chapzlock.core.ecs;

import org.chapzlock.core.ecs.Component;

import lombok.Getter;

/**
 * Lightweight view of an entity and its components.
 *
 * <p>Returned by {@link ComponentRegistry#view(Class[])} queries.
 * Components are cached at query time for fast iteration.
 */
public class EntityView {
    @Getter
    private final int id;
    private final Class<? extends Component>[] types;
    private final Component[] components;

    private EntityView(int id, Class<? extends Component>[] types, Component[] components) {
        this.id = id;
        this.types = types;
        this.components = components;
    }

    /**
     * Factory method for creating an entity view.
     *
     * @param id entity ID
     * @param types component types
     * @param components component instances
     * @return a new entity view
     */
    public static EntityView of(int id, Class<? extends Component>[] types, Component[] components) {
        return new EntityView(id, types, components);
    }

    /**
     * Retrieves a component from this view.
     *
     * @param type the component type
     * @return the component instance, or {@code null} if not present in the query
     * @param <T> component type
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T get(Class<T> type) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == type) {
                return (T) components[i];
            }
        }
        return null;
    }
}
