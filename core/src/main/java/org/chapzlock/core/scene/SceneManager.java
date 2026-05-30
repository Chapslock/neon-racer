package org.chapzlock.core.scene;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.chapzlock.core.application.EngineContext;
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

    public void load(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException(
                "Scene cannot be null. Pass a Scene built via Scene.builder(name).build().");
        }
        pendingTransitions.add(() -> doLoad(scene));
    }

    public void loadAdditive(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException(
                "Scene cannot be null. Pass a Scene built via Scene.builder(name).build().");
        }
        pendingTransitions.add(() -> doLoadAdditive(scene));
    }

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

    public void processPendingTransitions() {
        while (!pendingTransitions.isEmpty()) {
            pendingTransitions.poll().run();
        }
    }

    public List<Scene> getActiveScenes() {
        List<Scene> active = new ArrayList<>();
        if (primaryScene != null) {
            active.add(primaryScene);
        }
        active.addAll(additiveScenes);
        return active;
    }

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
        List<Integer> entityIds = new ArrayList<>();
        SceneAssets assets = new SceneAssets();
        for (EntityBlueprint blueprint : scene.getEntities()) {
            int entityId = Entity.create(registry);
            try {
                for (ComponentFactory factory : blueprint.getFactories()) {
                    Component[] components = factory.create(context);
                    for (Component component : components) {
                        registry.addComponent(entityId, component);
                        trackAsset(component, assets);
                    }
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
