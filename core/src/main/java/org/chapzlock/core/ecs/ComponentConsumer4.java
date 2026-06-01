package org.chapzlock.core.ecs;

/**
 * Callback for iterating entities with four component types.
 *
 * @param <A> first component type
 * @param <B> second component type
 * @param <C> third component type
 * @param <D> fourth component type
 * @see ComponentRegistry#forEach(Class, Class, Class, Class, ComponentConsumer4)
 */
@FunctionalInterface
public interface ComponentConsumer4<A extends Component, B extends Component,
    C extends Component, D extends Component> {
    void accept(int entityId, A a, B b, C c, D d);
}
