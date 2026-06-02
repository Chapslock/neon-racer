package org.chapzlock.core.ecs;

import java.util.List;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.Name;
import org.chapzlock.core.component.PbrMaterial;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Skeleton;
import org.chapzlock.core.component.SpriteMaterial;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.event.ComponentAddedEvent;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.scene.DescriptorResolver;
import org.chapzlock.core.scene.EntityBlueprint;

/**
 * Manages runtime entity creation through a phased assembly pipeline.
 *
 * <p>The assembly pipeline guarantees that events only fire <em>after</em> all components
 * are added to an entity (atomic commit). This eliminates ordering bugs where systems
 * (like PhysicsSystem) react to half-built entities.
 *
 * <p>Pipeline phases:
 * <ol>
 *   <li><strong>Resolve</strong> — converts descriptors (PhysicsDescriptor, SpriteDescriptor)
 *       into concrete runtime components via {@link DescriptorResolver}</li>
 *   <li><strong>Validate</strong> — checks component dependency rules (e.g., Mesh requires
 *       Transform). Invalid entities are rejected with a clear error message.</li>
 *   <li><strong>Commit</strong> — creates the entity ID, bulk-adds all components silently
 *       (no events), then fires {@link ComponentAddedEvent} for each component.</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * // Via builder (preferred for runtime spawning)
 * int id = entitySystem.builder("bullet")
 *     .transform(Transform.at(x, y, z))
 *     .component(new Velocity(dir))
 *     .instantiate();
 *
 * // Via blueprint (used internally by scene loading)
 * int id = entitySystem.instantiate(blueprint);
 * }</pre>
 *
 * @see EntityBuilder#instantiate()
 * @see DescriptorResolver
 * @see ComponentRule
 */
public class EntitySystem implements GameSystem {

    private static final List<ComponentRule> RULES = List.of(
        ComponentRule.requires(Mesh.class, Transform.class,
            "Mesh requires Transform"),
        ComponentRule.requires(PhysicsBody.class, Transform.class,
            "PhysicsBody requires Transform"),
        ComponentRule.requires(PointLight.class, Transform.class,
            "PointLight requires Transform"),
        ComponentRule.requires(SpriteMaterial.class, Mesh.class,
            "SpriteMaterial requires Mesh"),
        ComponentRule.requires(PbrMaterial.class, Mesh.class,
            "PbrMaterial requires Mesh"),
        ComponentRule.requires(Skeleton.class, Mesh.class,
            "Skeleton requires Mesh")
    );

    private ComponentRegistry registry;
    private DescriptorResolver descriptorResolver;

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.descriptorResolver = new DescriptorResolver(ctx);
    }

    /**
     * Creates a new {@link EntityBuilder} linked to this system, enabling the
     * {@link EntityBuilder#instantiate()} terminal operation.
     *
     * @param name the entity name (nullable for anonymous entities)
     * @return a new builder ready for method chaining
     */
    public EntityBuilder builder(String name) {
        EntityBuilder b = new EntityBuilder(name);
        b.setEntitySystem(this);
        return b;
    }

    /**
     * Instantiates an entity from a blueprint through the phased assembly pipeline.
     *
     * <p>Phases: resolve descriptors, validate rules, commit atomically with deferred events.
     *
     * @param blueprint the entity blueprint to instantiate
     * @return the new entity ID, or -1 if validation failed
     */
    public int instantiate(EntityBlueprint blueprint) {
        // Phase 1: Resolve descriptors into concrete components
        List<Component> resolved = descriptorResolver.resolve(blueprint);

        // Phase 2: Validate component rules
        String violation = checkRules(resolved, blueprint.getName());
        if (violation != null) {
            Log.error("EntitySystem", "Entity '" + blueprint.getName() + "' failed validation: " + violation);
            return -1;
        }

        // Phase 3: Commit atomically — create entity, add all components silently, then fire events
        int entityId = Entity.create(registry);
        registry.addComponentsSilent(entityId, resolved);

        // Add Name component if the blueprint has a non-internal name
        if (blueprint.getName() != null && !blueprint.getName().startsWith("_")) {
            registry.addComponentsSilent(entityId, List.of(new Name(blueprint.getName())));
        }

        // Fire ComponentAddedEvent for each component (entity is now fully assembled)
        for (Component component : resolved) {
            registry.publishEvent(new ComponentAddedEvent(entityId, component.getClass()));
        }

        return entityId;
    }

    private String checkRules(List<? extends Component> components, String entityName) {
        for (ComponentRule rule : RULES) {
            if (rule.isViolated(components)) {
                return rule.message();
            }
        }
        return null;
    }

    @Override
    public void onUpdate(float deltaTime) {
        // Future: process pending deferred spawns (IO completion)
    }

    @Override
    public void onDestroy() {
        // No resources to clean up
    }
}
