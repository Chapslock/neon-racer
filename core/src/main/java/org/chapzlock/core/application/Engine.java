package org.chapzlock.core.application;

import java.util.List;

import org.chapzlock.core.animation.AnimationSystem;
import org.chapzlock.core.asset.gltf.GltfAssetRegistry;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityFactory;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.TransformSystem;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.physics.Physics;
import org.chapzlock.core.physics.PhysicsSystem;
import org.chapzlock.core.physics.PhysicsWorld;
import org.chapzlock.core.physics.PhysicsWorldConfig;
import org.chapzlock.core.render.CameraProjection;
import org.chapzlock.core.render.MeshRegistry;
import org.chapzlock.core.render.RenderSystem;
import org.chapzlock.core.render.ShaderRegistry;
import org.chapzlock.core.render.TextureRegistry;
import org.chapzlock.core.scene.SceneManager;
import org.chapzlock.core.tween.TweenRegistry;
import org.chapzlock.core.tween.TweenSystem;
import org.chapzlock.core.window.Window;

import lombok.Getter;

/**
 * Central container for all engine subsystems.
 *
 * <p>Created via {@link #create(ApplicationSpecification)} at startup.
 * Contains: {@code ComponentRegistry}, {@code EventBus}, {@code Window}, GPU registries
 * ({@code MeshRegistry}, {@code TextureRegistry}, {@code ShaderRegistry}),
 * {@code Time}, {@code TweenRegistry}, {@code SceneManager}, {@code CameraProjection},
 * {@code Physics}.
 *
 * <p>For unit tests, use {@link #testEngine()} to create a minimal context with no GPU subsystems.
 */
@Getter
public class Engine implements Disposable {

    private static final Log log = Log.create(Engine.class);
    private final ComponentRegistry registry;
    private final EventBus eventBus;
    private final MeshRegistry meshRegistry;
    private final TextureRegistry textureRegistry;
    private final ShaderRegistry shaderRegistry;
    private final Window window;
    private final Time time;
    private final TweenRegistry tweenRegistry;
    private final GltfAssetRegistry gltfAssetRegistry;
    private final List<GameSystem> engineSystems;
    /**
     * Set after construction due to circular dependency: SceneManager needs
     * Engine in its constructor, but Engine holds SceneManager.
     * This is intentional — set once during Application.run() and never changed after.
     */
    private SceneManager sceneManager;
    private Physics physics;
    private EntityFactory entityFactory;
    private CameraProjection cameraProjection;

    public Engine(ComponentRegistry registry,
           EventBus eventBus,
           MeshRegistry meshRegistry,
           TextureRegistry textureRegistry,
           ShaderRegistry shaderRegistry,
           Window window,
           Time time,
           TweenRegistry tweenRegistry,
           GltfAssetRegistry gltfAssetRegistry,
           List<GameSystem> engineSystems) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.meshRegistry = meshRegistry;
        this.textureRegistry = textureRegistry;
        this.shaderRegistry = shaderRegistry;
        this.window = window;
        this.time = time;
        this.tweenRegistry = tweenRegistry;
        this.gltfAssetRegistry = gltfAssetRegistry;
        this.engineSystems = engineSystems;
    }

    Engine(ComponentRegistry registry,
           EventBus eventBus,
           MeshRegistry meshRegistry,
           TextureRegistry textureRegistry,
           ShaderRegistry shaderRegistry,
           Window window,
           Time time,
           TweenRegistry tweenRegistry,
           GltfAssetRegistry gltfAssetRegistry,
           SceneManager sceneManager,
           CameraProjection cameraProjection,
           Physics physics,
           EntityFactory entityFactory,
           List<GameSystem> engineSystems) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.meshRegistry = meshRegistry;
        this.textureRegistry = textureRegistry;
        this.shaderRegistry = shaderRegistry;
        this.window = window;
        this.time = time;
        this.tweenRegistry = tweenRegistry;
        this.gltfAssetRegistry = gltfAssetRegistry;
        this.sceneManager = sceneManager;
        this.cameraProjection = cameraProjection;
        this.physics = physics;
        this.entityFactory = entityFactory;
        this.engineSystems = engineSystems;
    }

    /**
     * Creates a fully-initialized engine from an application specification.
     *
     * <p>This factory method performs the full initialization ceremony: GLFW init,
     * window creation, GPU registry setup, physics world construction, engine system
     * creation and initialization. The returned engine is ready for scene loading and
     * the main loop.
     *
     * @param spec application configuration (window, physics, initial scene)
     * @return a fully-initialized engine
     */
    public static Engine create(ApplicationSpecification spec) {
        ApplicationUtil.init();

        EventBus eventBus = new EventBus();
        ComponentRegistry registry = new ComponentRegistry(eventBus);

        Window window = new Window(spec.getWindowSpec(), eventBus);
        window.create(0);

        ShaderRegistry shaderRegistry = new ShaderRegistry();
        TextureRegistry textureRegistry = new TextureRegistry();
        MeshRegistry meshRegistry = new MeshRegistry();
        Time time = new Time();
        TweenRegistry tweenRegistry = new TweenRegistry();
        GltfAssetRegistry gltfAssetRegistry = new GltfAssetRegistry();
        CameraProjection cameraProjection = new CameraProjection(spec.getWindowSpec());

        // Create physics
        PhysicsWorldConfig physicsConfig = spec.getPhysicsConfig() != null
            ? spec.getPhysicsConfig() : PhysicsWorldConfig.defaults();
        PhysicsWorld physicsWorld = new PhysicsWorld(physicsConfig);
        physicsWorld.setMeshRegistry(meshRegistry);
        Physics physics = new Physics(physicsWorld);

        // Create engine systems
        List<GameSystem> engineSystems = List.of(
            new TweenSystem(),
            new AnimationSystem(),
            new TransformSystem(),
            new PhysicsSystem(physicsConfig, cameraProjection, physicsWorld),
            new RenderSystem()
        );

        // Assemble engine
        Engine engine = new Engine(
            registry, eventBus, meshRegistry, textureRegistry, shaderRegistry,
            window, time, tweenRegistry, gltfAssetRegistry,
            null, cameraProjection, physics, null, engineSystems);

        // Wire components that need the full Engine reference
        EntityFactory entityFactory = new EntityFactory();
        entityFactory.init(engine);
        engine.setEntityFactory(entityFactory);

        SceneManager sceneManager = new SceneManager(engine);
        engine.setSceneManager(sceneManager);
        sceneManager.registerEngineSystems(engineSystems);

        // Initialize engine systems
        engineSystems.forEach(s -> s.onInit(engine));

        log.info("Engine created: %s (%dx%d)",
            spec.getWindowSpec().getTitle(),
            spec.getWindowSpec().getWidth(),
            spec.getWindowSpec().getHeight());

        return engine;
    }

    /**
     * Creates a minimal engine for unit tests (no GPU systems).
     *
     * @return a test engine with {@code ComponentRegistry}, {@code EventBus}, {@code Time}, and {@code TweenRegistry}
     */
    public static Engine testEngine() {
        EventBus eventBus = new EventBus();
        Engine engine = new Engine(
            new ComponentRegistry(eventBus),
            eventBus,
            null,
            null,
            null,
            null,
            new Time(),
            new TweenRegistry(),
            new GltfAssetRegistry(),
            List.of()
        );
        EntityFactory entityFactory = new EntityFactory();
        entityFactory.init(engine);
        engine.setEntityFactory(entityFactory);
        SceneManager sceneManager = new SceneManager(engine);
        engine.setSceneManager(sceneManager);
        return engine;
    }

    @Override
    public void dispose() {
        if (sceneManager != null) sceneManager.unloadAll();
        if (engineSystems != null) {
            for (GameSystem system : engineSystems) {
                system.onDestroy();
            }
        }
        if (shaderRegistry != null) shaderRegistry.dispose();
        if (textureRegistry != null) textureRegistry.dispose();
        if (meshRegistry != null) meshRegistry.dispose();
        if (eventBus != null) eventBus.clearAllListeners();
        if (window != null) window.destroy();
    }

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public void setPhysics(Physics physics) {
        this.physics = physics;
    }

    public void setEntityFactory(EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    public void setCameraProjection(CameraProjection cameraProjection) {
        this.cameraProjection = cameraProjection;
    }
}
