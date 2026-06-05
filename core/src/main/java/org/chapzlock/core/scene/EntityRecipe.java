package org.chapzlock.core.scene;

import java.util.function.Consumer;

import org.chapzlock.core.ecs.EntityBuilder;

/**
 * A deferred entity definition — a name paired with a configuration function.
 *
 * <p>Recipes are the building blocks of {@link Scene} definitions. Rather than creating
 * entities immediately, a recipe stores <em>what</em> to create (name + component configuration)
 * so that {@link SceneManager} can instantiate entities later when the scene loads.
 *
 * <p>The configurator receives an {@link org.chapzlock.core.ecs.EntityBuilder} and attaches
 * components/tags to it. Example:
 * <pre>{@code
 * new EntityRecipe("player", builder -> builder
 *     .component(Transform.at(0, 1, 0))
 *     .component(new GltfModel("models/car.glb"))
 *     .tag(new PlayerTag()));
 * }</pre>
 *
 * @param name optional entity name (null for anonymous entities)
 * @param configurator function that attaches components to the entity builder
 * @see SceneBuilder#entity(String, java.util.function.Consumer)
 */
public record EntityRecipe(String name, Consumer<EntityBuilder> configurator) {}
