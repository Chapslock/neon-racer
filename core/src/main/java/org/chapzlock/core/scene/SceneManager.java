package org.chapzlock.core.scene;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.gltf.GltfAsset;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.logging.Log;

import lombok.Getter;

/**
 * Manages scene lifecycle: loading, unloading, and transitions.
 *
 * <p>Supports one primary scene plus zero or more additive scenes.
 * Engine systems (PhysicsSystem, RenderSystem) are always active;
 * scene systems are added/removed with their scene.
 *
 * <p>Transitions:
 * <ul>
 *   <li>{@code load(scene)} — queue deferred transition (old scene unloaded, new scene instantiated at end-of-frame)</li>
 *   <li>{@code loadAdditive(scene)} — add a persistent overlay scene</li>
 *   <li>{@code unload(scene)} — remove an additive scene</li>
 * </ul>
 *
 * <p>GPU assets (meshes, textures, shaders) use reference counting.
 * Shared assets survive until all referencing scenes are unloaded.
 */
public class SceneManager {

    private final EngineContext context;
    private final ComponentRegistry registry;
    private final EventBus eventBus;

    @Getter
    private Scene primaryScene;
    private final List<Scene> additiveScenes = new ArrayList<>();
    private final Map<Scene, List<Integer>> sceneEntities = new HashMap<>();
    private final Map<Scene, SceneAssets> sceneAssets = new HashMap<>();
    private final Map<Scene, List<GameSystem>> sceneSystems = new HashMap<>();
    private final List<GameSystem> engineSystems = new ArrayList<>();
    private final Queue<Runnable> pendingTransitions = new ArrayDeque<>();

    private List<GameSystem> cachedActiveSystems;
    private boolean activeSystemsDirty = true;

    public SceneManager(EngineContext context) {
        this.context = context;
        this.registry = context.getRegistry();
        this.eventBus = context.getEventBus();
    }

    public void registerEngineSystems(List<GameSystem> systems) {
        engineSystems.addAll(systems);
        activeSystemsDirty = true;
    }

    /**
     * Queues a deferred scene transition. The old scene is unloaded and the new scene
     * is loaded at end-of-frame via {@link #processPendingTransitions()}.
     *
     * @param scene the scene to load
     */
    public void load(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException(
                "Scene cannot be null. Pass a Scene built via Scene.builder(name).build().");
        }
        pendingTransitions.add(() -> doLoad(scene));
    }

    /**
     * Queues loading an additive scene. Additive scenes layer on top of the primary scene.
     * Use for HUD, pause menus, or debug overlays.
     *
     * @param scene the scene to load additively
     */
    public void loadAdditive(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException(
                "Scene cannot be null. Pass a Scene built via Scene.builder(name).build().");
        }
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

    public void loadImmediate(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException(
                "Scene cannot be null. Pass a Scene built via Scene.builder(name).build().");
        }
        doLoad(scene);
    }

    public void loadAdditiveImmediate(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException(
                "Scene cannot be null. Pass a Scene built via Scene.builder(name).build().");
        }
        doLoadAdditive(scene);
    }

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
        Log.debug("Loading glTF: " + resourcePath);
        GltfAsset asset = context.getGltfAssetRegistry().getOrLoad(resourcePath);
        GltfSceneInstantiator instantiator = new GltfSceneInstantiator(context);
        return instantiator.instantiate(asset);
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
     * @return list of active systems
     */
    public List<GameSystem> getActiveSystems() {
        if (activeSystemsDirty || cachedActiveSystems == null) {
            rebuildActiveSystemsList();
        }
        return cachedActiveSystems;
    }

    public void unloadAll() {
        if (primaryScene != null) {
            teardownScene(primaryScene);
            primaryScene = null;
        }
        for (Scene scene : new ArrayList<>(additiveScenes)) {
            teardownScene(scene);
        }
        additiveScenes.clear();
        activeSystemsDirty = true;
    }

    private void doLoad(Scene scene) {
        Log.debug("Loading scene \"" + scene.getName() + "\" (" + scene.getEntities().size() + " entities, " + scene.getSystems().size() + " systems)");
        if (primaryScene != null) {
            teardownScene(primaryScene);
            eventBus.publish(new SceneUnloadedEvent(primaryScene.getName()));
        }
        primaryScene = scene;
        instantiateScene(scene);
        eventBus.publish(new SceneLoadedEvent(scene.getName()));
        activeSystemsDirty = true;
    }

    private void doLoadAdditive(Scene scene) {
        Log.debug("Loading additive scene \"" + scene.getName() + "\"");
        for (Scene existing : additiveScenes) {
            if (existing.getName().equals(scene.getName())) {
                Log.warn("Scene \"" + scene.getName() + "\" is already loaded as additive. " +
                    "Ignoring duplicate loadAdditive() call. If you want to reload it, unload() first.");
                return;
            }
        }
        additiveScenes.add(scene);
        instantiateScene(scene);
        eventBus.publish(new SceneLoadedEvent(scene.getName()));
        activeSystemsDirty = true;
    }

    private void doUnload(Scene scene) {
        if (scene == primaryScene) {
            teardownScene(scene);
            primaryScene = null;
            eventBus.publish(new SceneUnloadedEvent(scene.getName()));
            activeSystemsDirty = true;
            return;
        }
        if (additiveScenes.remove(scene)) {
            teardownScene(scene);
            eventBus.publish(new SceneUnloadedEvent(scene.getName()));
            activeSystemsDirty = true;
        } else {
            List<String> activeNames = getActiveScenes().stream().map(Scene::getName).toList();
            Log.warn("Attempted to unload scene \"" + scene.getName() + "\" but it is not currently loaded. " +
                "Currently active scenes: " + activeNames + ". Check your scene transition logic.");
        }
    }

    private void instantiateScene(Scene scene) {
        DescriptorResolver resolver = new DescriptorResolver(context);
        List<Integer> entityIds = new ArrayList<>();
        SceneAssets assets = new SceneAssets();

        // If the scene has a glTF base, load it first.
        // The glTF nodes become entities alongside the programmatic blueprints.
        if (scene.getGltfPath() != null) {
            Log.debug("  Loading glTF base: " + scene.getGltfPath());
            GltfInstance gltfInstance = loadGltf(scene.getGltfPath());
            // Track all glTF entity IDs so they are destroyed when the scene is unloaded
            for (int entityId : gltfInstance.allEntities().values()) {
                entityIds.add(entityId);
            }
            // Also include the root entity if it wasn't already captured by name
            if (!entityIds.contains(gltfInstance.rootEntity())) {
                entityIds.add(gltfInstance.rootEntity());
            }
        }

        for (EntityBlueprint blueprint : scene.getEntities()) {
            int entityId = Entity.create(registry);
            try {
                List<Component> resolved = resolver.resolve(blueprint);
                for (Component component : resolved) {
                    registry.addComponent(entityId, component);
                    trackAsset(component, assets);
                }
                entityIds.add(entityId);
                Log.debug("  Entity \"" + blueprint.getName() + "\" created (id=" + entityId + ")");
            } catch (Exception e) {
                registry.destroyEntity(entityId);
                Log.error("Scene \"" + scene.getName() + "\", entity \"" + blueprint.getName() +
                    "\": Failed to instantiate. " + e.getMessage() +
                    " Ensure asset files exist in src/main/resources/ and paths are relative to the resources root.");
            }
        }
        retainAll(assets);
        sceneEntities.put(scene, entityIds);
        sceneAssets.put(scene, assets);

        for (int entityId : entityIds) {
            boolean hasMesh = registry.getComponent(entityId, Mesh.class) != null;
            boolean hasMaterial = registry.getComponent(entityId, Material.class) != null;
            boolean hasTransform = registry.getComponent(entityId, Transform.class) != null;
            boolean hasPhysicsBody = registry.getComponent(entityId, PhysicsBody.class) != null;

            if (hasMaterial && !hasMesh) {
                Log.warn("Entity " + entityId + " has Material but no Mesh — it won't be rendered.");
            }
            if (hasMesh && !hasTransform) {
                Log.warn("Entity " + entityId + " has Mesh but no Transform — it won't be rendered.");
            }
            if (hasPhysicsBody && !hasTransform) {
                Log.warn("Entity " + entityId + " has PhysicsBody but no Transform — physics registration will be skipped.");
            }
        }

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
        sceneSystems.put(scene, systems);

        InputMap inputMap = scene.getInputMap();
        if (inputMap != null) {
            inputMap.activate(eventBus);
            context.setActiveInputMap(inputMap);
        }

        if (scene.getEntities().isEmpty()) {
            Log.warn("Scene \"" + scene.getName() + "\" has no entities. " +
                "Did you forget to add .entity() calls to the builder?");
        }
    }

    private void teardownScene(Scene scene) {
        Log.debug("Tearing down scene \"" + scene.getName() + "\"");
        InputMap inputMap = scene.getInputMap();
        if (inputMap != null) {
            inputMap.deactivate();
            if (context.getActiveInputMap() == inputMap) {
                context.setActiveInputMap(null);
            }
        }

        List<GameSystem> systems = sceneSystems.remove(scene);
        if (systems != null) {
            systems.forEach(GameSystem::onDestroy);
        }

        List<Integer> entityIds = sceneEntities.remove(scene);
        if (entityIds != null) {
            for (int entityId : entityIds) {
                registry.destroyEntity(entityId);
            }
        }

        SceneAssets assets = sceneAssets.remove(scene);
        if (assets != null) {
            releaseAll(assets);
        }
    }

    private void rebuildActiveSystemsList() {
        List<GameSystem> all = new ArrayList<>(engineSystems);
        for (List<GameSystem> systems : sceneSystems.values()) {
            all.addAll(systems);
        }
        all.sort(Comparator.comparingInt(s -> s.phase().ordinal()));
        cachedActiveSystems = List.copyOf(all);
        activeSystemsDirty = false;
    }

    private void trackAsset(Component component, SceneAssets assets) {
        if (component instanceof Mesh mesh) {
            assets.meshIds.add(mesh.getId());
        } else if (component instanceof Material material) {
            for (Texture texture : material.getTextures()) {
                assets.textureIds.add(texture.getId());
            }
            if (material.getBlendMap() != null) {
                assets.textureIds.add(material.getBlendMap().getId());
            }
            if (material.getShader() != null && material.getShader().isCompiled()) {
                assets.shaderProgramIds.add(material.getShader().getProgramId());
            }
        }
    }

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

    private static class SceneAssets {
        final List<Integer> meshIds = new ArrayList<>();
        final List<Integer> textureIds = new ArrayList<>();
        final List<Integer> shaderProgramIds = new ArrayList<>();
    }
}
