package org.chapzlock.core.scene;

import java.util.function.Function;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.Component;

/**
 * Factory function for creating components at scene load time.
 *
 * <p>Invoked by {@link SceneManager} when instantiating entity blueprints.
 * Receives {@link EngineContext} to access GPU systems (MeshSystem, TextureSystem, etc).
 */
@FunctionalInterface
public interface ComponentFactory {
    /**
     * Creates components for an entity.
     *
     * @param ctx the engine context
     * @return array of components to attach
     */
    Component[] create(EngineContext ctx);

    /**
     * Helper for creating a single-component factory.
     *
     * @param fn function that creates a single component
     * @return a component factory
     */
    static ComponentFactory single(Function<EngineContext, Component> fn) {
        return ctx -> new Component[]{ fn.apply(ctx) };
    }
}
