package org.chapzlock.core.ecs;

import java.util.function.Consumer;

/**
 * A deferred child entity definition — a name paired with a configuration function.
 * Stored by EntityBuilder and instantiated after the parent entity is created.
 *
 * @param name optional child entity name (null for anonymous)
 * @param configurator function that attaches components to the child entity builder
 */
record ChildRecipe(String name, Consumer<EntityBuilder> configurator) {}
