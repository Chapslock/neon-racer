package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.gltf.GltfAsset;
import org.chapzlock.core.component.GltfModel;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.SpriteMaterial;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityBuilder;
import org.chapzlock.core.ecs.EntitySystem;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.logging.Log;

/**
 * Creates and tears down the runtime state of a scene.
 *
 * <p><strong>What this does:</strong> Takes a {@link Scene} blueprint and brings it to life
 * by delegating entity creation to {@link EntitySystem}, tracking GPU assets for reference
 * counting, loading glTF models as child entities, and initializing game systems. On teardown,
 * it reverses all of these steps.
 *
 * <p><strong>Why it exists:</strong> Previously, all of this logic lived inside
 * {@link SceneManager}, making it responsible for both lifecycle management (when scenes
 * load/unload) and instantiation mechanics (how scenes are created). Extracting instantiation
 * into this dedicated class gives SceneManager a single responsibility (lifecycle orchestration)
 * and makes instantiation independently testable.
 *
 * <p><strong>Delegation to EntitySystem:</strong> Entity creation (component resolution,
 * component rule validation, and atomic component commit) is fully handled by
 * {@link EntitySystem#instantiate(String, List, List)}. This class retains only scene-level
 * concerns: the entity loop, glTF loading, GPU asset tracking, and system lifecycle.
 *
 * <p><strong>Usage:</strong> This class is not used directly by game code. It is owned by
 * {@link SceneManager} and called internally during scene transitions.
 *
 * @see SceneInstance
 * @see SceneManager
 * @see EntitySystem
 */
public class SceneInstantiator {

    private final EngineContext context;
    private final ComponentRegistry registry;

    /**
     * Creates a new scene instantiator bound to the given engine context.
     *
     * <p>The instantiator uses the context to access the EntitySystem (for entity creation),
     * GPU systems (for asset reference counting), and the glTF asset registry
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
     *   <li>For each entity recipe, creates an EntityBuilder, applies the configurator,
     *       then delegates to {@link EntitySystem#instantiate(String, List, List)}
     *       which resolves components, validates rules, and commits atomically</li>
     *   <li>GPU asset IDs are tracked for reference counting</li>
     *   <li>If the entity has a GltfModel component, it is loaded as child entities</li>
     *   <li>All tracked GPU assets are retained (refcount incremented)</li>
     *   <li>Scene systems are initialized via {@code onInit()}</li>
     * </ol>
     *
     * <p><strong>Error handling:</strong> If an individual entity fails to instantiate
     * (e.g., missing asset file or validation failure), it is skipped. Other entities
     * continue loading. If a system's {@code onInit()} throws, that system is disabled
     * for this scene.
     *
     * @param scene the scene blueprint to instantiate
     * @return a {@link SceneInstance} containing all created entities, assets, and systems
     */
    public SceneInstance instantiate(Scene scene) {
        EntitySystem entitySystem = context.getEntitySystem();
        List<Integer> entityIds = new ArrayList<>();
        SceneAssets assets = new SceneAssets();

        for (EntityRecipe recipe : scene.getEntities()) {
            try {
                EntityBuilder builder = new EntityBuilder(recipe.name());
                recipe.configurator().accept(builder);

                int entityId = entitySystem.instantiate(
                    builder.getName(), builder.getComponents(), builder.getTags());

                if (entityId < 0) {
                    // Validation failed (e.g., missing required component dependency).
                    // EntitySystem already logged the error. Skip this entity.
                    continue;
                }

                entityIds.add(entityId);
                trackAssetsForEntity(entityId, assets);

                // If this entity has a GltfModel component, instantiate the model as children.
                // The glTF nodes become children of this entity, making it the "owner" of
                // the model. Animation clips are attached to this entity for easy access.
                GltfModel gltfModel = registry.getComponent(entityId, GltfModel.class);
                if (gltfModel != null) {
                    GltfAsset modelAsset = context.getGltfAssetRegistry().getOrLoad(gltfModel.resourcePath());
                    GltfModelFactory modelFactory = new GltfModelFactory(context);
                    GltfInstance modelInstance = modelFactory.instantiateAsChild(modelAsset, entityId);
                    for (int childId : modelInstance.allEntities().values()) {
                        entityIds.add(childId);
                    }
                    if (!entityIds.contains(modelInstance.rootEntity())) {
                        entityIds.add(modelInstance.rootEntity());
                    }
                }

                String name = recipe.name();
                Log.debug("  Entity \"" + (name != null ? name : "<anonymous>") + "\" created (id=" + entityId + ")");
            } catch (org.chapzlock.core.asset.gltf.GltfLoadException e) {
                String name = recipe.name();
                Log.error("Scene \"" + scene.getName() + "\", entity \"" + (name != null ? name : "<anonymous>") +
                    "\": glTF load failed for model asset. " + e.getMessage());
            } catch (IllegalStateException e) {
                String name = recipe.name();
                Log.error("Scene \"" + scene.getName() + "\", entity \"" + (name != null ? name : "<anonymous>") +
                    "\": Component validation error. " + e.getMessage());
            } catch (Exception e) {
                String name = recipe.name();
                Log.error("Scene \"" + scene.getName() + "\", entity \"" + (name != null ? name : "<anonymous>") +
                    "\": Unexpected error during instantiation. " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
        GltfModelFactory modelFactory = new GltfModelFactory(context);
        return modelFactory.instantiate(asset);
    }

    /**
     * Tracks GPU asset IDs for an entity by querying its components after instantiation.
     *
     * <p>Called after {@link EntitySystem#instantiate(String, List, List)} has committed all
     * components. Inspects the entity's Mesh and SpriteMaterial to record resource IDs
     * for later reference counting.
     *
     * @param entityId the entity to inspect for GPU resources
     * @param assets   the asset tracker to record IDs into
     */
    private void trackAssetsForEntity(int entityId, SceneAssets assets) {
        Mesh mesh = registry.getComponent(entityId, Mesh.class);
        if (mesh != null) {
            assets.meshIds.add(mesh.getId());
        }

        SpriteMaterial sprite = registry.getComponent(entityId, SpriteMaterial.class);
        if (sprite != null && sprite.texture() != null) {
            assets.textureIds.add(sprite.texture().getId());
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
        if (context.getMeshRegistry() == null) return;
        for (int id : assets.meshIds) {
            context.getMeshRegistry().retain(id);
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
        if (context.getMeshRegistry() == null) return;
        for (int id : assets.meshIds) {
            context.getMeshRegistry().release(id);
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
