package org.chapzlock.core.ecs;

/**
 * Callback for iterating entities with three component types.
 *
 * @param <A> first component type
 * @param <B> second component type
 * @param <C> third component type
 * @see ComponentRegistry#forEach(Class, Class, Class, ComponentConsumer3)
 */
@FunctionalInterface
public interface ComponentConsumer3<A extends Component, B extends Component, C extends Component> {
    void accept(int entityId, A a, B b, C c);
}
