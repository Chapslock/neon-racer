package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.ModelDescriptor;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneManagerTest {

    private EngineContext ctx;
    private SceneManager sceneManager;
    private EventBus eventBus;
    private List<Subscription> subscriptions;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        eventBus = ctx.getEventBus();
        sceneManager = new SceneManager(ctx);
        subscriptions = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        subscriptions.forEach(Subscription::unsubscribe);
    }

    private Scene simpleScene(String name) {
        return new Scene(name, List.of(
            new EntityBlueprintBuilder("entity1")
                .transform(Transform.at(0, 0, 0))
                .build()
        ), List.of(), null, 0.007f, 1.5f);
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

        ComponentRegistry registry = ctx.getRegistry();
        List<?> transforms = registry.view(Transform.class);
        assertEquals(1, transforms.size());
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
        ComponentRegistry registry = ctx.getRegistry();
        List<?> transforms = registry.view(Transform.class);
        assertEquals(1, transforms.size());
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

        ComponentRegistry registry = ctx.getRegistry();
        assertEquals(2, registry.view(Transform.class).size());

        sceneManager.unloadImmediate(additive);

        assertEquals(1, registry.view(Transform.class).size());
        assertEquals(1, sceneManager.getActiveScenes().size());
    }

    @Test
    void loadNullThrowsWithHelpfulMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
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
        public void onInit(EngineContext ctx) { initialized = true; }

        @Override
        public void onDestroy() { destroyed = true; }

        @Override
        public SystemPhase phase() { return SystemPhase.LOGIC; }
    }

    @Test
    void sceneSystemsAreInitializedOnLoadAndDestroyedOnUnload() {
        TrackingSystem system = new TrackingSystem();
        Scene scene = new Scene("sys-test", List.of(), List.of(system), null, 0.007f, 1.5f);

        sceneManager.loadImmediate(scene);
        assertTrue(system.initialized);

        sceneManager.loadImmediate(simpleScene("other"));
        assertTrue(system.destroyed);
    }

    @Test
    void getActiveSystemsIncludesEngineAndSceneSystems() {
        GameSystem engineSys = new TrackingSystem();
        TrackingSystem sceneSys = new TrackingSystem();

        sceneManager.registerEngineSystems(List.of(engineSys));
        Scene scene = new Scene("test", List.of(), List.of(sceneSys), null, 0.007f, 1.5f);
        sceneManager.loadImmediate(scene);

        List<GameSystem> active = sceneManager.getActiveSystems();
        assertTrue(active.contains(engineSys));
        assertTrue(active.contains(sceneSys));
    }

    @Test
    void activeSystemsSortedByPhase() {
        GameSystem renderSys = new GameSystem() {
            @Override public SystemPhase phase() { return SystemPhase.RENDER; }
        };
        GameSystem logicSys = new GameSystem() {};

        Scene scene = new Scene("test", List.of(), List.of(renderSys, logicSys), null, 0.007f, 1.5f);
        sceneManager.loadImmediate(scene);

        List<GameSystem> active = sceneManager.getActiveSystems();
        int logicIdx = active.indexOf(logicSys);
        int renderIdx = active.indexOf(renderSys);
        assertTrue(logicIdx < renderIdx, "LOGIC systems should come before RENDER");
    }

    @Test
    void loadRetainsAssetsAndUnloadReleasesAssets() {
        MeshSystem meshSystem = new MeshSystem();
        TextureSystem textureSystem = new TextureSystem();
        ShaderSystem shaderSystem = new ShaderSystem();
        EventBus eb = new EventBus();
        EngineContext assetCtx = EngineContext.builder()
            .registry(new ComponentRegistry(eb))
            .eventBus(eb)
            .meshSystem(meshSystem)
            .textureSystem(textureSystem)
            .shaderSystem(shaderSystem)
            .time(new org.chapzlock.core.application.Time())
            .tweenManager(new org.chapzlock.core.tween.TweenManager())
            .build();
        SceneManager sm = new SceneManager(assetCtx);

        Mesh fakeMesh = Mesh.builder().id(42).build();
        Texture fakeTexture = Texture.builder().id(7).build();
        Shader fakeShader = new Shader("v.vert", "f.frag");
        fakeShader.setProgramId(99);

        Scene scene = new Scene("asset-test", List.of(
            new EntityBlueprintBuilder("entity")
                .component(fakeMesh, new Material(fakeShader, fakeTexture))
                .build()
        ), List.of(), null, 0.007f, 1.5f);

        sm.loadImmediate(scene);

        assertEquals(1, meshSystem.getRefCount(42));
        assertEquals(1, textureSystem.getRefCount(7));
        assertEquals(1, shaderSystem.getRefCount(99));

        sm.loadImmediate(simpleScene("replace"));

        assertEquals(0, meshSystem.getRefCount(42));
        assertEquals(0, textureSystem.getRefCount(7));
        assertEquals(0, shaderSystem.getRefCount(99));
    }

    @Test
    void failedEntityInstantiationDoesNotLeavePartialEntity() {
        Scene scene = new Scene("bad-entity", List.of(
            new EntityBlueprintBuilder("good")
                .transform(Transform.at(0, 0, 0))
                .build(),
            new EntityBlueprintBuilder("bad")
                .transform(Transform.at(1, 1, 1))
                .model(ModelDescriptor.builder().mesh("nonexistent.obj").build())
                .build()
        ), List.of(), null, 0.007f, 1.5f);

        sceneManager.loadImmediate(scene);

        ComponentRegistry registry = ctx.getRegistry();
        List<?> transforms = registry.view(Transform.class);
        assertEquals(1, transforms.size(), "Only the good entity should exist");

        // The key test: when we unload the scene, it should only try to destroy
        // entities that were successfully created. This verifies that the failed
        // entity's ID was not added to the scene's entity tracking list.
        sceneManager.loadImmediate(simpleScene("replacement"));

        // After loading a replacement scene, the original scene's entities should be cleaned up
        // and the new scene should have its entity
        assertEquals(1, registry.view(Transform.class).size(), "New scene should have one entity");
    }
}
