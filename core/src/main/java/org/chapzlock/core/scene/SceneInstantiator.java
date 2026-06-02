package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.gltf.GltfAsset;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.Name;
import org.chapzlock.core.component.PbrMaterial;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Skeleton;
import org.chapzlock.core.component.SpriteMaterial;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.ComponentRule;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.logging.Log;

/**
 * Creates and tears down the runtime state of a scene.
 *
 * <p><strong>What this does:</strong> Takes a {@link Scene} blueprint and brings it to life
 * by creating ECS entities, resolving descriptors into GPU-backed components, validating
 * component dependencies, tracking GPU assets for reference counting, and initializing
 * game systems. On teardown, it reverses all of these steps.
 *
 * <p><strong>Why it exists:</strong> Previously, all of this logic lived inside
 * {@link SceneManager}, making it responsible for both lifecycle management (when scenes
 * load/unload) and instantiation mechanics (how scenes are created). Extracting instantiation
 * into this dedicated class gives SceneManager a single responsibility (lifecycle orchestration)
 * and makes instantiation independently testable.
 *
 * <p><strong>Component validation:</strong> After resolving each entity's components, this
 * class checks a set of {@link ComponentRule}s that declare required component dependencies.
 * For example, a {@code Mesh} requires a {@code Transform} — without it, the renderer
 * wouldn't know where to draw the mesh. If a rule is violated, the entity is destroyed and
 * skipped with a clear error message, preventing subtle rendering bugs.
 *
 * <p><strong>Usage:</strong> This class is not used directly by game code. It is owned by
 * {@link SceneManager} and called internally during scene transitions.
 *
 * @see SceneInstance
 * @see SceneManager
 * @see ComponentRule
 */
public class SceneInstantiator {

    /**
     * Validation rules checked against every entity's resolved component list.
     *
     * <p>Each rule declares: "if entity has component A, it MUST also have component B."
     * Violated entities are destroyed and excluded from the scene with a logged error.
     */
    private static final List<ComponentRule> RULES = List.of(
        ComponentRule.requires(Mesh.class, Transform.class,
            "Mesh requires Transform — entity won't be positioned in world space"),
        ComponentRule.requires(PhysicsBody.class, Transform.class,
            "PhysicsBody requires Transform — physics simulation needs initial position"),
        ComponentRule.requires(SpriteMaterial.class, Mesh.class,
            "SpriteMaterial requires Mesh — nothing to shade without geometry"),
        ComponentRule.requires(PbrMaterial.class, Mesh.class,
            "PbrMaterial requires Mesh — nothing to shade without geometry"),
        ComponentRule.requires(Skeleton.class, Mesh.class,
            "Skeleton requires Mesh — skinning needs geometry to deform")
    );

    private final EngineContext context;
    private final ComponentRegistry registry;

    /**
     * Creates a new scene instantiator bound to the given engine context.
     *
     * <p>The instantiator uses the context to access the component registry (for creating
     * entities), GPU systems (for asset reference counting), and the glTF asset registry
     * (for loading 3D models).
     *
     * @param context the engine context providing access to all subsystems
     */
    public SceneInstantiator(EngineContext context) {
        this.context = context;
        this.registry = context.getRegistry();
    }

    /**
     * Instantiates a scene blueprint into live ECS state.
     *
     * <p><strong>What happens during instantiation:</strong>
     * <ol>
     *   <li>For each entity blueprint in the scene, a new ECS entity is created</li>
     *   <li>Descriptors (sprite, physics) are resolved into GPU-backed components</li>
     *   <li>Component validation rules are checked — violating entities are destroyed</li>
     *   <li>GPU asset IDs are tracked for reference counting</li>
     *   <li>If the blueprint specifies a glTF model, it is loaded as child entities</li>
     *   <li>A {@link Name} component is added for named entities (for lookup by name)</li>
     *   <li>All tracked GPU assets are retained (refcount incremented)</li>
     *   <li>Scene systems are initialized via {@code onInit()}</li>
     * </ol>
     *
     * <p><strong>Error handling:</strong> If an individual entity fails to instantiate
     * (e.g., missing asset file), it is destroyed and excluded from the scene. Other
     * entities continue loading. If a system's {@code onInit()} throws, that system is
     * disabled for this scene.
     *
     * @param scene the scene blueprint to instantiate
     * @return a {@link SceneInstance} containing all created entities, assets, and systems
     */
    public SceneInstance instantiate(Scene scene) {
        DescriptorResolver resolver = new DescriptorResolver(context);
        List<Integer> entityIds = new ArrayList<>();
        SceneAssets assets = new SceneAssets();

        for (EntityBlueprint blueprint : scene.getEntities()) {
            int entityId = Entity.create(registry);
            try {
                List<Component> resolved = resolver.resolve(blueprint);

                // Validate component rules before committing components to the entity.
                // This catches misconfigured blueprints early with clear error messages.
                String violation = checkRules(resolved, blueprint.getName(), scene.getName());
                if (violation != null) {
                    registry.destroyEntity(entityId);
                    Log.error(violation);
                    continue;
                }

                for (Component component : resolved) {
                    registry.addComponent(entityId, component);
                    trackAsset(component, assets);
                }
                entityIds.add(entityId);

                if (!blueprint.getName().startsWith("_")) {
                    registry.addComponent(entityId, new Name(blueprint.getName()));
                }

                // If this entity has a glTF model path, instantiate the model as children.
                // The glTF nodes become children of this entity, making it the "owner" of
                // the model. Animation clips are attached to this entity for easy access.
                if (blueprint.getGltfModelPath() != null) {
                    GltfAsset modelAsset = context.getGltfAssetRegistry().getOrLoad(blueprint.getGltfModelPath());
                    GltfSceneInstantiator gltfInstantiator = new GltfSceneInstantiator(context);
                    GltfInstance modelInstance = gltfInstantiator.instantiateAsChild(modelAsset, entityId);
                    for (int childId : modelInstance.allEntities().values()) {
                        entityIds.add(childId);
                    }
                    if (!entityIds.contains(modelInstance.rootEntity())) {
                        entityIds.add(modelInstance.rootEntity());
                    }
                }

                Log.debug("  Entity \"" + blueprint.getName() + "\" created (id=" + entityId + ")");
            } catch (Exception e) {
                registry.destroyEntity(entityId);
                Log.error("Scene \"" + scene.getName() + "\", entity \"" + blueprint.getName() +
                    "\": Failed to instantiate. " + e.getMessage() +
                    " Ensure asset files exist in src/main/resources/ and paths are relative to the resources root.");
            }
        }
        retainAll(assets);

        List<GameSystem> systems = initSystems(scene);

        if (scene.getEntities().isEmpty()) {
            Log.warn("Scene \"" + scene.getName() + "\" has no entities. " +
                "Did you forget to add .entity() calls to the builder?");
        }

        return new SceneInstance(scene, entityIds, assets, systems);
    }

    /**
     * Tears down a scene instance, destroying all entities, releasing GPU assets,
     * and calling {@code onDestroy()} on all systems.
     *
     * <p><strong>Order of teardown:</strong>
     * <ol>
     *   <li>Systems are destroyed (so they can clean up subscriptions/state)</li>
     *   <li>Entities are destroyed (removes them from the ECS)</li>
     *   <li>GPU assets are released (decrements reference counts)</li>
     * </ol>
     *
     * <p>This method is safe to call even if some fields in the instance are empty
     * (e.g., a scene with no entities or no systems).
     *
     * @param instance the scene instance to tear down
     */
    public void teardown(SceneInstance instance) {
        List<GameSystem> systems = instance.systems();
        if (systems != null) {
            systems.forEach(GameSystem::onDestroy);
        }

        List<Integer> entityIds = instance.entityIds();
        if (entityIds != null) {
            for (int entityId : entityIds) {
                registry.destroyEntity(entityId);
            }
        }

        SceneAssets assets = instance.assets();
        if (assets != null) {
            releaseAll(assets);
        }
    }

    /**
     * Loads an entire glTF file as entities in the current scene context.
     *
     * <p>This creates entities for every node in the glTF file, uploads meshes to the GPU,
     * creates PBR materials, and sets up the parent-child hierarchy. Returns a handle for
     * looking up entities by their glTF node names (as set in Blender or other DCC tools).
     *
     * <p><strong>When to use:</strong> Called by {@link SceneManager#loadGltf(String)} to
     * delegate the actual loading work. Game code should call SceneManager's method, not
     * this one directly.
     *
     * <p><strong>Asset lifecycle:</strong> Meshes, textures, and shaders loaded by this method
     * are managed by the GPU systems' reference counting. They will be released when the
     * entities are destroyed (typically when the scene is unloaded).
     *
     * @param resourcePath path relative to the resources root (e.g., "models/track.glb")
     * @return a {@link GltfInstance} handle for name-based entity lookup
     * @throws org.chapzlock.core.asset.gltf.GltfLoadException if the file cannot be loaded or parsed
     */
    public GltfInstance loadGltf(String resourcePath) {
        Log.debug("Loading glTF: " + resourcePath);
        GltfAsset asset = context.getGltfAssetRegistry().getOrLoad(resourcePath);
        GltfSceneInstantiator gltfInstantiator = new GltfSceneInstantiator(context);
        return gltfInstantiator.instantiate(asset);
    }

    /**
     * Checks all component validation rules against a resolved component list.
     *
     * <p>Returns a formatted error message if any rule is violated, or {@code null} if
     * all rules pass. Only the first violation is reported (entities typically have one
     * fundamental configuration error, not many).
     *
     * @param components the resolved components for an entity
     * @param entityName the blueprint name (for error messages)
     * @param sceneName  the scene name (for error messages)
     * @return error message string if violated, or null if all rules pass
     */
    private String checkRules(List<Component> components, String entityName, String sceneName) {
        for (ComponentRule rule : RULES) {
            if (rule.isViolated(components)) {
                return "Scene \"" + sceneName + "\", entity \"" + entityName +
                    "\": " + rule.message() + ". Entity skipped.";
            }
        }
        return null;
    }

    /**
     * Records a component's GPU resource ID in the scene's asset tracker.
     *
     * <p>Only components that hold GPU resources need tracking. Currently:
     * <ul>
     *   <li>{@link Mesh} — tracks the mesh buffer ID</li>
     *   <li>{@link SpriteMaterial} — tracks the texture ID (if present)</li>
     * </ul>
     *
     * @param component the component that may hold a GPU resource
     * @param assets    the asset tracker to record IDs into
     */
    private void trackAsset(Component component, SceneAssets assets) {
        if (component instanceof Mesh mesh) {
            assets.meshIds.add(mesh.getId());
        } else if (component instanceof SpriteMaterial spriteMaterial) {
            if (spriteMaterial.texture() != null) {
                assets.textureIds.add(spriteMaterial.texture().getId());
            }
        }
    }

    /**
     * Retains (increments reference count) all GPU assets tracked by a scene.
     *
     * <p>Called after all entities in a scene have been instantiated, ensuring the GPU
     * systems know these resources are in active use and should not be freed.
     *
     * @param assets the scene's tracked asset IDs
     */
    private void retainAll(SceneAssets assets) {
        if (context.getMeshSystem() == null) return;
        for (int id : assets.meshIds) {
            context.getMeshSystem().retain(id);
        }
        for (int id : assets.textureIds) {
            context.getTextureSystem().retain(id);
        }
        for (int id : assets.shaderProgramIds) {
            context.getShaderSystem().retain(id);
        }
    }

    /**
     * Releases (decrements reference count) all GPU assets tracked by a scene.
     *
     * <p>Called when a scene is unloaded. If the reference count for a resource reaches
     * zero, the GPU system will free the underlying OpenGL resource.
     *
     * @param assets the scene's tracked asset IDs
     */
    private void releaseAll(SceneAssets assets) {
        if (context.getMeshSystem() == null) return;
        for (int id : assets.meshIds) {
            context.getMeshSystem().release(id);
        }
        for (int id : assets.textureIds) {
            context.getTextureSystem().release(id);
        }
        for (int id : assets.shaderProgramIds) {
            context.getShaderSystem().release(id);
        }
    }

    /**
     * Initializes scene systems and returns the list of successfully initialized ones.
     *
     * <p>Systems that throw during {@code onInit()} are logged and excluded from the
     * returned list (disabled for this scene load). This prevents a single broken system
     * from blocking the entire scene.
     *
     * @param scene the scene whose systems to initialize
     * @return list of successfully initialized systems
     */
    private List<GameSystem> initSystems(Scene scene) {
        List<GameSystem> systems = new ArrayList<>(scene.getSystems());
        List<GameSystem> failedSystems = new ArrayList<>();
        for (GameSystem system : systems) {
            try {
                system.onInit(context);
            } catch (Exception e) {
                Log.error("Scene \"" + scene.getName() + "\": " + system.getClass().getSimpleName() +
                    ".onInit() threw exception. This system will be disabled. " +
                    "Fix the error in onInit() and reload the scene. Error: " + e.getMessage());
                failedSystems.add(system);
            }
        }
        systems.removeAll(failedSystems);
        return systems;
    }
}
