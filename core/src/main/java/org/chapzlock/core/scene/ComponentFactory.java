package org.chapzlock.core.scene;

import java.util.function.Function;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.Component;

@FunctionalInterface
public interface ComponentFactory {
    Component[] create(EngineContext ctx);

    static ComponentFactory single(Function<EngineContext, Component> fn) {
        return ctx -> new Component[]{ fn.apply(ctx) };
    }
}
