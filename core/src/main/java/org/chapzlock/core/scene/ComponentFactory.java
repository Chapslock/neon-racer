package org.chapzlock.core.scene;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.Component;

@FunctionalInterface
public interface ComponentFactory {
    Component[] create(EngineContext ctx);
}
