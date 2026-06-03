package org.chapzlock.core.scene;

import java.util.function.Consumer;

import org.chapzlock.core.ecs.EntityBuilder;

public record EntityRecipe(String name, Consumer<EntityBuilder> configurator) {}
