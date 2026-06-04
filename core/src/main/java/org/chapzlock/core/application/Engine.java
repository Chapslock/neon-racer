package org.chapzlock.core.application;

import org.chapzlock.core.animation.AnimationSystem;
import org.chapzlock.core.asset.gltf.GltfAssetRegistry;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityFactory;
import org.chapzlock.core.ecs.SystemScheduler;
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
    private final SystemScheduler scheduler;
    private final TweenSystem tweenSystem;
    private final AnimationSystem animationSystem;
    private final TransformSystem transformSystem;
    private final PhysicsSystem physicsSystem;
    private final RenderSystem renderSystem;
    private final EntityFactory entityFactory;
    private final CameraProjection cameraProjection;
    private final Physics physics;
    /**
     * Set once via {@link #initSceneManager(SceneManager)} due to circular dependency:
     * SceneManager needs Engine in its constructor, but Engine holds SceneManager.
     */
    private SceneManager sceneManager;

    public Engine(ComponentRegistry registry,
           EventBus eventBus,
           MeshRegistry meshRegistry,
           TextureRegistry textureRegistry,
           ShaderRegistry shaderRegistry,
           Window window,
           Time time,
           TweenRegistry tweenRegistry,
           GltfAssetRegistry gltfAssetRegistry,
           SystemScheduler scheduler,
           EntityFactory entityFactory,
           CameraProjection cameraProjection,
           Physics physics,
           TweenSystem tweenSystem,
           AnimationSystem animationSystem,
           TransformSystem transformSystem,
           PhysicsSystem physicsSystem,
           RenderSystem renderSystem) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.meshRegistry = meshRegistry;
        this.textureRegistry = textureRegistry;
        this.shaderRegistry = shaderRegistry;
        this.window = window;
        this.time = time;
        this.tweenRegistry = tweenRegistry;
        this.gltfAssetRegistry = gltfAssetRegistry;
        this.scheduler = scheduler;
        this.entityFactory = entityFactory;
        this.cameraProjection = cameraProjection;
        this.physics = physics;
        this.tweenSystem = tweenSystem;
        this.animationSystem = animationSystem;
        this.transformSystem = transformSystem;
        this.physicsSystem = physicsSystem;
        this.renderSystem = renderSystem;
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

        // Create scheduler and permanent systems
        SystemScheduler scheduler = new SystemScheduler();
        TweenSystem tweenSystem = new TweenSystem();
        AnimationSystem animationSystem = new AnimationSystem();
        TransformSystem transformSystem = new TransformSystem();
        PhysicsSystem physicsSystem = new PhysicsSystem(physicsConfig, cameraProjection, physicsWorld);
        RenderSystem renderSystem = new RenderSystem();

        // EntityFactory
        EntityFactory entityFactory = new EntityFactory(registry, meshRegistry, textureRegistry, shaderRegistry);

        // Assemble engine (immutable after this — except sceneManager)
        Engine engine = new Engine(
            registry, eventBus, meshRegistry, textureRegistry, shaderRegistry,
            window, time, tweenRegistry, gltfAssetRegistry,
            scheduler, entityFactory, cameraProjection, physics,
            tweenSystem, animationSystem, transformSystem, physicsSystem, renderSystem);

        // SceneManager needs Engine for system.onInit(engine) — the one circular dep
        SceneManager sceneManager = new SceneManager(engine, scheduler);
        engine.initSceneManager(sceneManager);

        // Register permanent systems into scheduler
        scheduler.addPermanent(tweenSystem);
        scheduler.addPermanent(animationSystem);
        scheduler.addPermanent(transformSystem);
        scheduler.addPermanent(physicsSystem);
        scheduler.addPermanent(renderSystem);

        // Initialize permanent systems
        tweenSystem.onInit(engine);
        animationSystem.onInit(engine);
        transformSystem.onInit(engine);
        physicsSystem.onInit(engine);
        renderSystem.onInit(engine);

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
        ComponentRegistry registry = new ComponentRegistry(eventBus);
        SystemScheduler scheduler = new SystemScheduler();
        GltfAssetRegistry gltfAssetRegistry = new GltfAssetRegistry();
        EntityFactory entityFactory = new EntityFactory(registry, null, null, null);

        Engine engine = new Engine(
            registry, eventBus, null, null, null, null,
            new Time(), new TweenRegistry(), gltfAssetRegistry,
            scheduler, entityFactory, null, null,
            null, null, null, null, null);

        SceneManager sceneManager = new SceneManager(engine, scheduler);
        engine.initSceneManager(sceneManager);

        return engine;
    }

    /**
     * Creates a minimal engine for unit tests that need physics (no GPU systems).
     *
     * <p>Unlike {@link #testEngine()}, this includes a functioning PhysicsSystem and Physics
     * instance. Use this when testing systems that interact with the physics world.
     *
     * @return a test engine with physics, ComponentRegistry, EventBus, Time, and TweenRegistry
     */
    public static Engine testEngineWithPhysics() {
        EventBus eventBus = new EventBus();
        ComponentRegistry registry = new ComponentRegistry(eventBus);
        SystemScheduler scheduler = new SystemScheduler();
        GltfAssetRegistry gltfAssetRegistry = new GltfAssetRegistry();
        EntityFactory entityFactory = new EntityFactory(registry, null, null, null);

        PhysicsWorldConfig physicsConfig = PhysicsWorldConfig.defaults();
        PhysicsWorld physicsWorld = new PhysicsWorld(physicsConfig);
        Physics physics = new Physics(physicsWorld);
        PhysicsSystem physicsSystem = new PhysicsSystem(physicsConfig, null, physicsWorld);

        Engine engine = new Engine(
            registry, eventBus, null, null, null, null,
            new Time(), new TweenRegistry(), gltfAssetRegistry,
            scheduler, entityFactory, null, physics,
            null, null, null, physicsSystem, null);

        SceneManager sceneManager = new SceneManager(engine, scheduler);
        engine.initSceneManager(sceneManager);

        physicsSystem.onInit(engine);

        return engine;
    }

    @Override
    public void dispose() {
        if (sceneManager != null) sceneManager.unloadAll();
        if (renderSystem != null) renderSystem.onDestroy();
        if (physicsSystem != null) physicsSystem.onDestroy();
        if (transformSystem != null) transformSystem.onDestroy();
        if (animationSystem != null) animationSystem.onDestroy();
        if (tweenSystem != null) tweenSystem.onDestroy();
        if (shaderRegistry != null) shaderRegistry.dispose();
        if (textureRegistry != null) textureRegistry.dispose();
        if (meshRegistry != null) meshRegistry.dispose();
        if (eventBus != null) eventBus.clearAllListeners();
        if (window != null) window.destroy();
    }

    /**
     * Sets the SceneManager — the single allowed mutation after construction.
     * Package-private; can only be called once. Exists because SceneManager needs Engine
     * in its constructor (for {@code system.onInit(engine)} calls), creating a circular dependency.
     *
     * @param sceneManager the scene manager to set
     * @throws IllegalStateException if called more than once
     */
    void initSceneManager(SceneManager sceneManager) {
        if (this.sceneManager != null) {
            throw new IllegalStateException("SceneManager already initialized");
        }
        this.sceneManager = sceneManager;
    }
}
