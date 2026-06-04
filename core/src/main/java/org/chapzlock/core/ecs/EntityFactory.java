package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.asset.RawMeshDataFactory;
import org.chapzlock.core.asset.gltf.GltfAssetManager;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.GltfModel;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.Name;
import org.chapzlock.core.component.PbrMaterial;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Skeleton;
import org.chapzlock.core.component.SpriteLayer;
import org.chapzlock.core.component.SpriteMaterial;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.event.ComponentAddedEvent;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.render.MeshRegistry;
import org.chapzlock.core.render.TextureRegistry;

/**
 * Manages runtime entity creation through a phased assembly pipeline.
 *
 * <p>The assembly pipeline guarantees that events only fire <em>after</em> all components
 * are added to an entity (atomic commit). This eliminates ordering bugs where systems
 * (like PhysicsSystem) react to half-built entities.
 *
 * <p>Pipeline phases:
 * <ol>
 *   <li><strong>Resolve</strong> — resolves deferred components (e.g., SpriteMaterial with
 *       a texture path is loaded into a GPU texture)</li>
 *   <li><strong>Validate</strong> — checks component dependency rules (e.g., Mesh requires
 *       Transform). Invalid entities are rejected with a clear error message.</li>
 *   <li><strong>Commit</strong> — creates the entity ID, bulk-adds all components silently
 *       (no events), then fires {@link ComponentAddedEvent} for each component.</li>
 *   <li><strong>glTF expansion</strong> — if the entity has a {@link GltfModel} component,
 *       delegates to {@link GltfAssetManager#instantiate(String, int)} which handles the
 *       full five-phase glTF pipeline (entities, hierarchy, meshes, skeletons, animations).</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * // Via builder (preferred for runtime spawning)
 * int id = entityFactory.builder("bullet")
 *     .transform(Transform.at(x, y, z))
 *     .component(new Velocity(dir))
 *     .instantiate();
 *
 * // Via direct call (used internally by scene loading)
 * int id = entityFactory.instantiate(name, components, tags);
 * }</pre>
 *
 * @see EntityBuilder#instantiate()
 * @see ComponentRule
 * @see GltfAssetManager
 */
public class EntityFactory {

    private static final Log log = Log.create(EntityFactory.class);

    private final ComponentRegistry registry;
    private final MeshRegistry meshRegistry;
    private final TextureRegistry textureRegistry;
    private final GltfAssetManager gltfAssetManager;

    public EntityFactory(ComponentRegistry registry,
                         MeshRegistry meshRegistry,
                         TextureRegistry textureRegistry,
                         GltfAssetManager gltfAssetManager) {
        this.registry = registry;
        this.meshRegistry = meshRegistry;
        this.textureRegistry = textureRegistry;
        this.gltfAssetManager = gltfAssetManager;
        registerDefaultRules();
    }

    private void registerDefaultRules() {
        registry.addRule(ComponentRule.requires(Camera.class, Transform.class, "Camera requires Transform"));
        registry.addRule(ComponentRule.requires(Mesh.class, Transform.class, "Mesh requires Transform"));
        registry.addRule(ComponentRule.requires(PhysicsBody.class, Transform.class, "PhysicsBody requires Transform"));
        registry.addRule(ComponentRule.requires(PointLight.class, Transform.class, "PointLight requires Transform"));
        registry.addRule(ComponentRule.requires(SpriteMaterial.class, Mesh.class, "SpriteMaterial requires Mesh"));
        registry.addRule(ComponentRule.requires(PbrMaterial.class, Mesh.class, "PbrMaterial requires Mesh"));
        registry.addRule(ComponentRule.requires(Skeleton.class, Mesh.class, "Skeleton requires Mesh"));
    }

    /**
     * Creates a new {@link EntityBuilder} linked to this factory, enabling the
     * {@link EntityBuilder#instantiate()} terminal operation.
     *
     * @param name the entity name (nullable for anonymous entities)
     * @return a new builder ready for method chaining
     */
    public EntityBuilder builder(String name) {
        EntityBuilder b = new EntityBuilder(name);
        b.setEntityFactory(this);
        return b;
    }

    /**
     * Instantiates an entity from component and tag lists through the phased assembly pipeline.
     *
     * <p>Phases: resolve components, validate rules, commit atomically with deferred events.
     *
     * @param name       the entity name (nullable for anonymous entities)
     * @param components the components to attach
     * @param tags       the tags to attach
     * @return the new entity ID, or -1 if validation failed
     */
    public int instantiate(String name, List<Component> components, List<Tag> tags) {
        // Phase 1: Resolve deferred components
        List<Component> resolved = resolveComponents(components, tags);

        // Phase 2: Validate component rules
        String violation = checkRules(resolved, name);
        if (violation != null) {
            log.error("Entity '%s' failed validation: %s", name, violation);
            return -1;
        }

        // Phase 3: Commit atomically — create entity, add all components silently, then fire events
        int entityId = Entity.create(registry);
        registry.addComponentsSilent(entityId, resolved);

        // Add Name component if the entity has a non-internal name
        if (name != null && !name.startsWith("_")) {
            registry.addComponentsSilent(entityId, List.of(new Name(name)));
        }

        // Phase 4: glTF expansion — if entity has GltfModel, expand as child hierarchy
        if (gltfAssetManager != null) {
            GltfModel gltfModel = registry.getComponent(entityId, GltfModel.class);
            if (gltfModel != null) {
                gltfAssetManager.instantiate(gltfModel.resourcePath(), entityId);
            }
        }

        // Fire ComponentAddedEvent for each component (entity is now fully assembled)
        for (Component component : resolved) {
            registry.publishEvent(new ComponentAddedEvent(entityId, component.getClass()));
        }

        return entityId;
    }


    // ──────────────────────────────────────────────────────────────────────────────
    // Private helpers — simple entity pipeline
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Resolves deferred components into their runtime equivalents.
     *
     * <p>Handles:
     * <ul>
     *   <li>SpriteMaterial with needsResolution() — loads texture via TextureRegistry</li>
     *   <li>SpriteLayer present without Mesh — auto-creates UI quad mesh</li>
     *   <li>Tags — added to the result list as components</li>
     * </ul>
     */
    private List<Component> resolveComponents(List<Component> components, List<Tag> tags) {
        List<Component> result = new ArrayList<>();
        boolean hasMesh = false;
        boolean hasSpriteLayer = false;

        for (Component c : components) {
            if (c instanceof SpriteMaterial sm && sm.needsResolution()) {
                Texture loaded = this.textureRegistry.load(sm.texturePath());
                result.add(sm.withTexture(loaded));
            } else {
                result.add(c);
            }
            if (c instanceof Mesh) hasMesh = true;
            if (c instanceof SpriteLayer) hasSpriteLayer = true;
        }

        // Auto-create UI quad mesh if SpriteLayer is present without Mesh
        if (hasSpriteLayer && !hasMesh) {
            Mesh mesh = this.meshRegistry.load(RawMeshDataFactory.createUi());
            result.add(mesh);
        }

        // Add tags to result list
        for (Tag tag : tags) {
            result.add(tag);
        }

        return result;
    }

    private String checkRules(List<? extends Component> components, String entityName) {
        for (ComponentRule rule : registry.getRules()) {
            if (rule.isViolated(components)) {
                return rule.message();
            }
        }
        return null;
    }

}
