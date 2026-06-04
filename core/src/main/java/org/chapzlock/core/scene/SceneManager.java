package org.chapzlock.core.scene;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemScheduler;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.logging.Log;

import lombok.Getter;

/**
 * Manages scene lifecycle: loading, unloading, and transitions.
 *
 * <p><strong>What this does:</strong> Orchestrates when scenes load and unload, manages
 * the transition queue, maintains the active systems cache, and handles input map
 * activation/deactivation. The actual work of creating entities, tracking GPU assets,
 * and initializing systems is delegated to {@link SceneInstantiator}.
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
 * reference counting managed by {@link SceneInstantiator}. Shared assets survive until
 * all referencing scenes are unloaded.
 *
 * @see SceneInstantiator
 * @see SceneInstance
 * @see Scene
 */
public class SceneManager {

    private static final Log log = Log.create(SceneManager.class);

    private final Engine engine;
    private final EventBus eventBus;
    private final SceneInstantiator instantiator;
    private final SystemScheduler scheduler;

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
     * <p>Internally creates a {@link SceneInstantiator} that handles the mechanics of
     * entity creation, asset tracking, and system initialization.
     *
     * @param engine the engine providing access to all subsystems
     * @param scheduler the system scheduler for managing active systems
     */
    public SceneManager(Engine engine, SystemScheduler scheduler) {
        this.engine = engine;
        this.eventBus = engine.getEventBus();
        this.instantiator = new SceneInstantiator(engine);
        this.scheduler = scheduler;
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
     * Loads an entire glTF file as entities in the current scene context.
     *
     * <p>This creates entities for every node in the glTF file, uploads meshes to the GPU,
     * creates PBR materials, and sets up the parent-child hierarchy. Returns a handle for
     * looking up entities by their glTF node names (as set in Blender or other DCC tools).
     *
     * <p><strong>When to use:</strong> Call this when you want to spawn a 3D model at runtime
     * without making it part of a scene's blueprint. For example, spawning a car prefab when
     * a player joins, or loading level geometry during a cutscene.
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * GltfInstance track = sceneManager.loadGltf("models/track.glb");
     * int player = track.entity("Player");      // Look up by Blender node name
     * int camera = track.entity("MainCamera");  // Another named node
     * }</pre>
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
        return instantiator.loadGltf(resourcePath);
    }

    /**
     * Loads a glTF file as a prefab and returns the root entity ID.
     *
     * <p>This is a convenience method that wraps {@link #loadGltf(String)} for simpler use
     * cases where you only need the root entity and don't need name-based lookup. The root
     * entity represents the top of the glTF node hierarchy — moving it will move the entire
     * loaded model.
     *
     * <p><strong>When to use:</strong> Use this when you need to load a model and only
     * interact with it as a single entity (e.g., a static prop, a collectible, a projectile).
     * If you need to access individual named nodes inside the model, use
     * {@link #loadGltf(String)} instead.
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * int carEntity = sceneManager.instantiateGltf("models/car.glb");
     * Transform t = registry.getComponent(carEntity, Transform.class);
     * t.setPosition(spawnPoint);
     * }</pre>
     *
     * @param resourcePath path relative to the resources root (e.g., "models/car.glb")
     * @return the root entity ID of the instantiated glTF scene
     * @throws org.chapzlock.core.asset.gltf.GltfLoadException if the file cannot be loaded or parsed
     */
    public int instantiateGltf(String resourcePath) {
        return loadGltf(resourcePath).rootEntity();
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
     * Delegates to the instantiator and handles input map activation.
     *
     * <p>Input map activation is a lifecycle concern (it ties the scene's input bindings
     * to the event bus), so it stays here in SceneManager rather than in SceneInstantiator.
     */
    private void doInstantiate(Scene scene) {
        SceneInstance instance = instantiator.instantiate(scene);
        loadedScenes.put(scene, instance);
        scheduler.addSceneSystems(instance.systems());

        InputMap inputMap = scene.getInputMap();
        if (inputMap != null) {
            inputMap.activate(eventBus);
            this.activeInputMap = inputMap;
        }
    }

    /**
     * Handles input map deactivation and delegates teardown to the instantiator.
     *
     * <p>Input map deactivation must happen before entity destruction, since input
     * handlers may reference entities that are about to be destroyed.
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
            instantiator.teardown(instance);
        }
    }
}
