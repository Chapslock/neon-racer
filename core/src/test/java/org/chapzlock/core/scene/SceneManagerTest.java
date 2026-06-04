package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.SpriteMaterial;
import org.chapzlock.core.component.SpriteLayer;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.render.MeshRegistry;
import org.chapzlock.core.render.TextureRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneManagerTest {

    private Engine engine;
    private SceneManager sceneManager;
    private EventBus eventBus;
    private List<Subscription> subscriptions;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        eventBus = engine.getEventBus();
        sceneManager = engine.getSceneManager();
        subscriptions = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        subscriptions.forEach(Subscription::unsubscribe);
    }

    private Scene simpleScene(String name) {
        return new Scene(name, List.of(
            new EntityRecipe("entity1", e -> e.transform(Transform.at(0, 0, 0)))
        ), List.of(), null);
    }

    @Test
    void loadImmediateSetsPrimaryScene() {
        Scene scene = simpleScene("test");
        sceneManager.loadImmediate(scene);

        assertSame(scene, sceneManager.getPrimaryScene());
    }

    @Test
    void loadImmediateCreatesEntitiesInRegistry() {
        Scene scene = simpleScene("test");
        sceneManager.loadImmediate(scene);

        ComponentRegistry registry = engine.getRegistry();
        assertEquals(1, registry.count(Transform.class));
    }

    @Test
    void loadImmediatePublishesSceneLoadedEvent() {
        List<SceneLoadedEvent> events = new ArrayList<>();
        subscriptions.add(eventBus.subscribe(SceneLoadedEvent.class, event -> {
            events.add(event);
            return false;
        }));

        sceneManager.loadImmediate(simpleScene("my-scene"));

        assertEquals(1, events.size());
        assertEquals("my-scene", events.get(0).sceneName());
    }

    @Test
    void loadImmediateReplacesExistingPrimaryScene() {
        sceneManager.loadImmediate(simpleScene("first"));
        sceneManager.loadImmediate(simpleScene("second"));

        assertEquals("second", sceneManager.getPrimaryScene().getName());
        ComponentRegistry registry = engine.getRegistry();
        assertEquals(1, registry.count(Transform.class));
    }

    @Test
    void loadImmediatePublishesUnloadThenLoadEvents() {
        sceneManager.loadImmediate(simpleScene("first"));

        List<String> eventOrder = new ArrayList<>();
        subscriptions.add(eventBus.subscribe(SceneUnloadedEvent.class, event -> {
            eventOrder.add("unload:" + event.sceneName());
            return false;
        }));
        subscriptions.add(eventBus.subscribe(SceneLoadedEvent.class, event -> {
            eventOrder.add("load:" + event.sceneName());
            return false;
        }));

        sceneManager.loadImmediate(simpleScene("second"));

        assertEquals(List.of("unload:first", "load:second"), eventOrder);
    }

    @Test
    void loadAdditiveDoesNotReplacePrimary() {
        Scene primary = simpleScene("primary");
        Scene additive = simpleScene("hud");

        sceneManager.loadImmediate(primary);
        sceneManager.loadAdditiveImmediate(additive);

        assertSame(primary, sceneManager.getPrimaryScene());
        assertEquals(2, sceneManager.getActiveScenes().size());
    }

    @Test
    void unloadAdditiveRemovesOnlyItsEntities() {
        Scene primary = simpleScene("primary");
        Scene additive = simpleScene("hud");

        sceneManager.loadImmediate(primary);
        sceneManager.loadAdditiveImmediate(additive);

        ComponentRegistry registry = engine.getRegistry();
        assertEquals(2, registry.count(Transform.class));

        sceneManager.unloadImmediate(additive);

        assertEquals(1, registry.count(Transform.class));
        assertEquals(1, sceneManager.getActiveScenes().size());
    }

    @Test
    void loadNullThrowsWithHelpfulMessage() {
        NullPointerException ex = assertThrows(NullPointerException.class, () ->
            sceneManager.loadImmediate(null));
        assertTrue(ex.getMessage().contains("Scene cannot be null"));
    }

    @Test
    void deferredLoadExecutesOnProcessPendingTransitions() {
        Scene scene = simpleScene("deferred");
        sceneManager.load(scene);

        assertNull(sceneManager.getPrimaryScene());

        sceneManager.processPendingTransitions();

        assertSame(scene, sceneManager.getPrimaryScene());
    }

    static class TrackingSystem implements GameSystem {
        boolean initialized = false;
        boolean destroyed = false;

        @Override
        public void onInit(Engine engine) { initialized = true; }

        @Override
        public void onDestroy() { destroyed = true; }

        @Override
        public SystemPhase phase() { return SystemPhase.LOGIC; }
    }

    @Test
    void sceneSystemsAreInitializedOnLoadAndDestroyedOnUnload() {
        TrackingSystem system = new TrackingSystem();
        Scene scene = new Scene("sys-test", List.of(), List.of(system), null);

        sceneManager.loadImmediate(scene);
        assertTrue(system.initialized);

        sceneManager.loadImmediate(simpleScene("other"));
        assertTrue(system.destroyed);
    }

    @Test
    void getActiveSystemsIncludesSceneSystems() {
        TrackingSystem sceneSys = new TrackingSystem();

        Scene scene = new Scene("test", List.of(), List.of(sceneSys), null);
        sceneManager.loadImmediate(scene);

        List<GameSystem> active = sceneManager.getActiveSystems();
        assertTrue(active.contains(sceneSys));
    }

    @Test
    void activeSystemsSortedByPhase() {
        GameSystem renderSys = new GameSystem() {
            @Override public SystemPhase phase() { return SystemPhase.RENDER; }
        };
        GameSystem logicSys = new GameSystem() {};

        Scene scene = new Scene("test", List.of(), List.of(renderSys, logicSys), null);
        sceneManager.loadImmediate(scene);

        List<GameSystem> active = sceneManager.getActiveSystems();
        int logicIdx = active.indexOf(logicSys);
        int renderIdx = active.indexOf(renderSys);
        assertTrue(logicIdx < renderIdx, "LOGIC systems should come before RENDER");
    }

    @Test
    void loadRetainsAssetsAndUnloadReleasesAssets() {
        MeshRegistry meshRegistry = new MeshRegistry();
        TextureRegistry textureRegistry = new TextureRegistry();
        EventBus eb = new EventBus();
        ComponentRegistry reg = new ComponentRegistry(eb);
        org.chapzlock.core.ecs.SystemScheduler scheduler = new org.chapzlock.core.ecs.SystemScheduler();
        org.chapzlock.core.ecs.EntityFactory entityFactory = new org.chapzlock.core.ecs.EntityFactory(
            reg, meshRegistry, textureRegistry, null);
        Engine assetEngine = new Engine(
            reg, eb, meshRegistry, textureRegistry, null, null,
            new org.chapzlock.core.application.Time(),
            new org.chapzlock.core.tween.TweenRegistry(),
            new org.chapzlock.core.asset.gltf.GltfAssetRegistry(),
            scheduler, entityFactory, null, null,
            null, null, null, null, null
        );
        SceneManager sm = new SceneManager(assetEngine, scheduler);

        Mesh fakeMesh = Mesh.builder().id(42).build();
        Texture fakeTexture = Texture.builder().id(7).build();

        Scene scene = new Scene("asset-test", List.of(
            new EntityRecipe("entity", e -> e
                .transform(Transform.at(0, 0, 0))
                .component(fakeMesh, new SpriteMaterial(fakeTexture)))
        ), List.of(), null);

        sm.loadImmediate(scene);

        assertEquals(1, meshRegistry.getRefCount(42));
        assertEquals(1, textureRegistry.getRefCount(7));

        sm.loadImmediate(simpleScene("replace"));

        assertEquals(0, meshRegistry.getRefCount(42));
        assertEquals(0, textureRegistry.getRefCount(7));
    }

    @Test
    void loadImmediateAddsNameComponentForNamedRecipes() {
        Scene scene = new Scene("test", List.of(
            new EntityRecipe("player", e -> e.transform(Transform.at(0, 0, 0)))
        ), List.of(), null);

        sceneManager.loadImmediate(scene);

        ComponentRegistry registry = engine.getRegistry();
        int playerId = registry.getEntityByName("player");
        assertNotEquals(-1, playerId);
    }

    @Test
    void failedEntityInstantiationDoesNotLeavePartialEntity() {
        Scene scene = new Scene("bad-entity", List.of(
            new EntityRecipe("good", e -> e.transform(Transform.at(0, 0, 0))),
            new EntityRecipe("bad", e -> e
                .transform(Transform.at(1, 1, 1))
                .component(new SpriteMaterial("nonexistent.png"))
                .component(new SpriteLayer()))
        ), List.of(), null);

        sceneManager.loadImmediate(scene);

        ComponentRegistry registry = engine.getRegistry();
        assertEquals(1, registry.count(Transform.class), "Only the good entity should exist");

        // The key test: when we unload the scene, it should only try to destroy
        // entities that were successfully created. This verifies that the failed
        // entity's ID was not added to the scene's entity tracking list.
        sceneManager.loadImmediate(simpleScene("replacement"));

        // After loading a replacement scene, the original scene's entities should be cleaned up
        // and the new scene should have its entity
        assertEquals(1, registry.count(Transform.class), "New scene should have one entity");
    }
}
