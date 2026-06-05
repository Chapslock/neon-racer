package org.chapzlock.core.scene;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.asset.gltf.GltfAssetManager;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.SpriteMaterial;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.ecs.EntityBuilder;
import org.chapzlock.core.ecs.EntityFactory;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemScheduler;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.render.MeshRegistry;
import org.chapzlock.core.render.ShaderRegistry;
import org.chapzlock.core.render.TextureRegistry;

import lombok.Getter;

/**
 * Manages scene lifecycle: loading, unloading, transitions, and instantiation.
 *
 * <p><strong>What this does:</strong> Orchestrates when scenes load and unload, manages
 * the transition queue, maintains the active systems cache, handles input map
 * activation/deactivation, and performs the mechanics of entity creation, GPU asset
 * tracking, and system initialization directly.
 *
 * <p><strong>Scene types:</strong>
 * <ul>
 *   <li><em>Primary scene</em> — the main gameplay scene (only one at a time)</li>
 *   <li><em>Additive scenes</em> — overlays that layer on top (HUD, pause menu, debug)</li>
 * </ul>
 *
 * <p><strong>Transition modes:</strong>
 * <ul>
 *   <li>{@code load(scene)} — queue deferred transition (old scene unloaded, new scene
 *       instantiated at end-of-frame via {@link #processPendingTransitions()})</li>
 *   <li>{@code loadImmediate(scene)} — synchronous load (used for initial scene)</li>
 *   <li>{@code loadAdditive(scene)} — add a persistent overlay scene</li>
 *   <li>{@code unload(scene)} — remove a scene (primary or additive)</li>
 * </ul>
 *
 * <p><strong>GPU asset lifecycle:</strong> GPU assets (meshes, textures, shaders) use
 * reference counting managed internally. Shared assets survive until all referencing
 * scenes are unloaded.
 *
 * @see SceneInstance
 * @see Scene
 */
public class SceneManager {

    private static final Log log = Log.create(SceneManager.class);

    private final Engine engine;
    private final EventBus eventBus;
    private final SystemScheduler scheduler;
    private final ComponentRegistry registry;
    private final EntityFactory entityFactory;
    private final GltfAssetManager gltfAssetManager;
    private final MeshRegistry meshRegistry;
    private final TextureRegistry textureRegistry;
    private final ShaderRegistry shaderRegistry;

    @Getter
    private Scene primaryScene;
    @Getter
    private InputMap activeInputMap;
    private final List<Scene> additiveScenes = new ArrayList<>();
    private final Map<Scene, SceneInstance> loadedScenes = new HashMap<>();
    private final Queue<Runnable> pendingTransitions = new ArrayDeque<>();

    /**
     * Creates a new scene manager bound to the given engine context.
     *
     * @param engine the engine providing access to all subsystems
     * @param scheduler the system scheduler for managing active systems
     */
    public SceneManager(Engine engine, SystemScheduler scheduler) {
        this.engine = engine;
        this.eventBus = engine.getEventBus();
        this.scheduler = scheduler;
        this.registry = engine.getRegistry();
        this.entityFactory = engine.getEntityFactory();
        this.gltfAssetManager = engine.getGltfAssetManager();
        this.meshRegistry = engine.getMeshRegistry();
        this.textureRegistry = engine.getTextureRegistry();
        this.shaderRegistry = engine.getShaderRegistry();
    }

    /**
     * Sets the currently active input map (used by game systems to read input).
     *
     * @param inputMap the input map to activate, or null to clear
     */
    public void setActiveInputMap(InputMap inputMap) {
        this.activeInputMap = inputMap;
    }

    /**
     * Queues a deferred scene transition. The old scene is unloaded and the new scene
     * is loaded at end-of-frame via {@link #processPendingTransitions()}.
     *
     * <p><strong>When to use:</strong> During gameplay, when you want to switch scenes
     * without disrupting the current frame's update/render cycle. The transition happens
     * cleanly between frames.
     *
     * @param scene the scene to load
     * @throws IllegalArgumentException if scene is null
     */
    public void load(Scene scene) {
        Objects.requireNonNull(scene, "Scene cannot be null. Pass a Scene built via Scene.builder(name).build().");
        pendingTransitions.add(() -> doLoad(scene));
    }

    /**
     * Queues loading an additive scene. Additive scenes layer on top of the primary scene.
     *
     * <p><strong>When to use:</strong> For HUD overlays, pause menus, debug panels, or
     * any content that should coexist with the primary scene rather than replace it.
     *
     * @param scene the scene to load additively
     * @throws IllegalArgumentException if scene is null
     */
    public void loadAdditive(Scene scene) {
        Objects.requireNonNull(scene, "Scene cannot be null. Pass a Scene built via Scene.builder(name).build().");
        pendingTransitions.add(() -> doLoadAdditive(scene));
    }

    /**
     * Queues unloading a scene (primary or additive).
     *
     * @param scene the scene to unload
     */
    public void unload(Scene scene) {
        pendingTransitions.add(() -> doUnload(scene));
    }

    /**
     * Loads a scene synchronously (blocks until fully instantiated).
     *
     * <p><strong>When to use:</strong> For the initial scene load at application startup,
     * or in tests. During gameplay, prefer {@link #load(Scene)} for deferred transitions.
     *
     * @param scene the scene to load immediately
     * @throws IllegalArgumentException if scene is null
     */
    public void loadImmediate(Scene scene) {
        Objects.requireNonNull(scene, "Scene cannot be null. Pass a Scene built via Scene.builder(name).build().");
        doLoad(scene);
    }

    /**
     * Loads an additive scene synchronously.
     *
     * @param scene the scene to load additively
     * @throws IllegalArgumentException if scene is null
     */
    public void loadAdditiveImmediate(Scene scene) {
        Objects.requireNonNull(scene, "Scene cannot be null. Pass a Scene built via Scene.builder(name).build().");
        doLoadAdditive(scene);
    }

    /**
     * Unloads a scene synchronously.
     *
     * @param scene the scene to unload
     */
    public void unloadImmediate(Scene scene) {
        doUnload(scene);
    }

    /**
     * Loads a glTF file at runtime, creating an anchor entity and expanding the model
     * hierarchy as children via GltfAssetManager.
     *
     * <p><strong>When to use:</strong> Call this when you want to spawn a 3D model at runtime
     * without making it part of a scene's blueprint. For example, spawning a car prefab when
     * a player joins, or loading level geometry during a cutscene.
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * int carEntity = sceneManager.loadGltf("models/car.glb");
     * Transform t = registry.getComponent(carEntity, Transform.class);
     * t.setPosition(spawnPoint);
     * }</pre>
     *
     * <p><strong>Asset lifecycle:</strong> Meshes, textures, and shaders loaded by this method
     * are managed by the GPU systems' reference counting. They will be released when the
     * entities are destroyed (typically when the scene is unloaded).
     *
     * @param resourcePath path relative to the resources root (e.g., "models/track.glb")
     * @return the root entity ID of the instantiated glTF scene
     * @throws org.chapzlock.core.asset.gltf.GltfLoadException if the file cannot be loaded or parsed
     */
    public int loadGltf(String resourcePath) {
        log.debug("Loading glTF: %s", resourcePath);
        int rootEntity = Entity.create(registry);
        registry.addComponent(rootEntity, new Transform());
        gltfAssetManager.instantiate(resourcePath, rootEntity);
        return rootEntity;
    }

    /**
     * Loads a glTF file as a prefab and returns the root entity ID.
     *
     * <p>This is a convenience alias for {@link #loadGltf(String)}.
     *
     * @param resourcePath path relative to the resources root (e.g., "models/car.glb")
     * @return the root entity ID of the instantiated glTF scene
     * @throws org.chapzlock.core.asset.gltf.GltfLoadException if the file cannot be loaded or parsed
     */
    public int instantiateGltf(String resourcePath) {
        return loadGltf(resourcePath);
    }

    /**
     * Processes all queued scene transitions. Called by the main loop at end-of-frame.
     *
     * <p><strong>How it works:</strong> Transitions queued via {@link #load(Scene)},
     * {@link #loadAdditive(Scene)}, and {@link #unload(Scene)} are stored in a FIFO queue.
     * This method drains the queue, executing each transition in order. This ensures
     * transitions happen at a safe point between frames, not mid-update.
     */
    public void processPendingTransitions() {
        while (!pendingTransitions.isEmpty()) {
            pendingTransitions.poll().run();
        }
    }

    /**
     * Returns all active scenes (primary + additive).
     *
     * @return list of active scenes
     */
    public List<Scene> getActiveScenes() {
        List<Scene> active = new ArrayList<>();
        if (primaryScene != null) {
            active.add(primaryScene);
        }
        active.addAll(additiveScenes);
        return active;
    }

    /**
     * Returns all active systems (engine + scene systems), sorted by phase.
     *
     * <p>The result is cached by the scheduler and only rebuilt when scenes are loaded/unloaded.
     * Engine systems always come before scene systems of the same phase.
     *
     * @return immutable list of active systems sorted by {@code SystemPhase} ordinal
     */
    public List<GameSystem> getActiveSystems() {
        return scheduler.getActiveSystems();
    }

    /**
     * Unloads all scenes (primary and all additive).
     *
     * <p><strong>When to use:</strong> During application shutdown or when resetting
     * the entire game state.
     */
    public void unloadAll() {
        if (primaryScene != null) {
            doTeardown(primaryScene);
            primaryScene = null;
        }
        for (Scene scene : new ArrayList<>(additiveScenes)) {
            doTeardown(scene);
        }
        additiveScenes.clear();
    }

    private void doLoad(Scene scene) {
        log.info("--- Loading scene \"%s\" ---", scene.getName());
        long startMs = System.currentTimeMillis();
        if (primaryScene != null) {
            doTeardown(primaryScene);
            eventBus.publish(new SceneUnloadedEvent(primaryScene.getName()));
        }
        primaryScene = scene;
        doInstantiate(scene);
        long elapsed = System.currentTimeMillis() - startMs;
        SceneInstance instance = loadedScenes.get(scene);
        int entityCount = instance != null ? instance.entityIds().size() : 0;
        int systemCount = instance != null ? instance.systems().size() : 0;
        log.info("Scene \"%s\" loaded in %dms (%d entities, %d systems)",
            scene.getName(), elapsed, entityCount, systemCount);
        eventBus.publish(new SceneLoadedEvent(scene.getName()));
    }

    private void doLoadAdditive(Scene scene) {
        log.debug("Loading additive scene \"%s\"", scene.getName());
        for (Scene existing : additiveScenes) {
            if (existing.getName().equals(scene.getName())) {
                log.warn("Scene \"%s\" is already loaded as additive. Ignoring duplicate loadAdditive() call. If you want to reload it, unload() first.",
                    scene.getName());
                return;
            }
        }
        additiveScenes.add(scene);
        doInstantiate(scene);
        eventBus.publish(new SceneLoadedEvent(scene.getName()));
    }

    private void doUnload(Scene scene) {
        if (scene == primaryScene) {
            doTeardown(scene);
            primaryScene = null;
            eventBus.publish(new SceneUnloadedEvent(scene.getName()));
            return;
        }
        if (additiveScenes.remove(scene)) {
            doTeardown(scene);
            eventBus.publish(new SceneUnloadedEvent(scene.getName()));
        } else {
            List<String> activeNames = getActiveScenes().stream().map(Scene::getName).toList();
            log.warn("Attempted to unload scene \"%s\" but it is not currently loaded. Currently active scenes: %s. Check your scene transition logic.",
                scene.getName(), activeNames);
        }
    }

    /**
     * Instantiates a scene blueprint into live ECS state and activates its input map.
     *
     * <p><strong>What happens during instantiation:</strong>
     * <ol>
     *   <li>For each entity recipe, creates an EntityBuilder, applies the configurator,
     *       then delegates to {@link EntityFactory#instantiate(String, List, List)}
     *       which resolves components, validates rules, and commits atomically
     *       (GltfModel expansion is handled automatically by EntityFactory)</li>
     *   <li>GPU asset IDs are tracked for reference counting</li>
     *   <li>All tracked GPU assets are retained (refcount incremented)</li>
     *   <li>Scene systems are initialized via {@code onInit()}</li>
     *   <li>The scene's input map is activated on the event bus</li>
     * </ol>
     */
    private void doInstantiate(Scene scene) {
        List<Integer> entityIds = new ArrayList<>();
        SceneAssets assets = new SceneAssets();

        for (EntityRecipe recipe : scene.getEntityRecipes()) {
            try {
                EntityBuilder builder = EntityBuilder.create(recipe.name());
                recipe.configurator().accept(builder);

                int entityId = entityFactory.instantiate(builder);

                if (entityId < 0) {
                    continue;
                }

                entityIds.add(entityId);
                trackAssetsForEntity(entityId, assets);

                String name = recipe.name();
                log.debug("  Entity \"%s\" created (id=%d)", name != null ? name : "<anonymous>", entityId);
            } catch (org.chapzlock.core.asset.gltf.GltfLoadException e) {
                String name = recipe.name();
                log.error("Scene \"%s\", entity \"%s\": glTF load failed for model asset. %s",
                    scene.getName(), name != null ? name : "<anonymous>", e.getMessage());
            } catch (IllegalStateException e) {
                String name = recipe.name();
                log.error("Scene \"%s\", entity \"%s\": Component validation error. %s",
                    scene.getName(), name != null ? name : "<anonymous>", e.getMessage());
            } catch (Exception e) {
                String name = recipe.name();
                log.error("Scene \"%s\", entity \"%s\": Unexpected error during instantiation. %s: %s",
                    scene.getName(), name != null ? name : "<anonymous>", e.getClass().getSimpleName(), e.getMessage());
            }
        }
        retainAll(assets);

        List<GameSystem> systems = initSystems(scene);

        if (scene.getEntityRecipes().isEmpty()) {
            log.warn("Scene \"%s\" has no entities — did you forget .entity() calls in the builder?",
                scene.getName());
        }

        if (!scene.getEntityRecipes().isEmpty() && scene.getSystems().isEmpty()) {
            log.warn("Scene \"%s\" has %d entities but no systems — nothing will update them",
                scene.getName(), scene.getEntityRecipes().size());
        }

        SceneInstance instance = new SceneInstance(scene, entityIds, assets, systems);
        loadedScenes.put(scene, instance);
        scheduler.addSceneSystems(instance.systems());

        InputMap inputMap = scene.getInputMap();
        if (inputMap != null) {
            inputMap.activate(eventBus);
            this.activeInputMap = inputMap;
        }
    }

    /**
     * Tears down a scene instance: deactivates input map, destroys systems, entities,
     * and releases GPU assets.
     *
     * <p><strong>Order of teardown:</strong>
     * <ol>
     *   <li>Input map is deactivated (before entity destruction, since handlers may reference entities)</li>
     *   <li>Systems are removed from the scheduler and destroyed</li>
     *   <li>Entities are destroyed (removed from the ECS)</li>
     *   <li>GPU assets are released (decrements reference counts)</li>
     * </ol>
     */
    private void doTeardown(Scene scene) {
        log.info("--- Unloading scene \"%s\" ---", scene.getName());
        InputMap inputMap = scene.getInputMap();
        if (inputMap != null) {
            inputMap.deactivate();
            if (this.activeInputMap == inputMap) {
                this.activeInputMap = null;
            }
        }

        SceneInstance instance = loadedScenes.remove(scene);
        if (instance != null) {
            scheduler.removeSceneSystems(instance.systems());

            List<GameSystem> systems = instance.systems();
            if (systems != null) {
                systems.forEach(GameSystem::onDestroy);
            }

            List<Integer> entityIds = instance.entityIds();
            if (entityIds != null) {
                for (int entityId : entityIds) {
                    if (registry.entityExists(entityId)) {
                        entityFactory.destroyEntity(entityId);
                    }
                }
            }

            SceneAssets assets = instance.assets();
            if (assets != null) {
                releaseAll(assets);
            }
        }
    }

    /**
     * Tracks GPU asset IDs for an entity by querying its components after instantiation.
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
     */
    private void retainAll(SceneAssets assets) {
        if (meshRegistry == null) return;
        for (int id : assets.meshIds) {
            meshRegistry.retain(id);
        }
        for (int id : assets.textureIds) {
            textureRegistry.retain(id);
        }
        for (int id : assets.shaderProgramIds) {
            shaderRegistry.retain(id);
        }
    }

    /**
     * Releases (decrements reference count) all GPU assets tracked by a scene.
     */
    private void releaseAll(SceneAssets assets) {
        if (meshRegistry == null) return;
        for (int id : assets.meshIds) {
            meshRegistry.release(id);
        }
        for (int id : assets.textureIds) {
            textureRegistry.release(id);
        }
        for (int id : assets.shaderProgramIds) {
            shaderRegistry.release(id);
        }
    }

    /**
     * Initializes scene systems and returns the list of successfully initialized ones.
     *
     * <p>Systems that throw during {@code onInit()} are logged and excluded from the
     * returned list (disabled for this scene load).
     */
    private List<GameSystem> initSystems(Scene scene) {
        List<GameSystem> systems = new ArrayList<>(scene.getSystems());
        List<GameSystem> failedSystems = new ArrayList<>();
        for (GameSystem system : systems) {
            try {
                system.onInit(engine);
            } catch (Exception e) {
                log.error("Scene \"%s\": %s.onInit() threw exception. This system will be disabled. Fix the error in onInit() and reload the scene. Error: %s",
                    scene.getName(), system.getClass().getSimpleName(), e.getMessage());
                failedSystems.add(system);
            }
        }
        systems.removeAll(failedSystems);
        return systems;
    }
}
