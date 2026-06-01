package org.chapzlock.core.ecs;

/**
 * Callback for iterating entities with one component type.
 *
 * @param <A> the component type
 * @see ComponentRegistry#forEach(Class, ComponentConsumer1)
 */
@FunctionalInterface
public interface ComponentConsumer1<A extends Component> {
    void accept(int entityId, A a);
}
