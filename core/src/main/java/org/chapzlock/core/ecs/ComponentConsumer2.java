package org.chapzlock.core.ecs;

/**
 * Callback for iterating entities with two component types.
 *
 * @param <A> first component type
 * @param <B> second component type
 * @see ComponentRegistry#forEach(Class, Class, ComponentConsumer2)
 */
@FunctionalInterface
public interface ComponentConsumer2<A extends Component, B extends Component> {
    void accept(int entityId, A a, B b);
}
