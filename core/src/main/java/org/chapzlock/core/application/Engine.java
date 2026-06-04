package org.chapzlock.core.application;

import java.util.List;

import org.chapzlock.core.asset.gltf.GltfAssetRegistry;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntitySystem;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.physics.Physics;
import org.chapzlock.core.render.CameraSystem;
import org.chapzlock.core.render.MeshRegistry;
import org.chapzlock.core.render.ShaderRegistry;
import org.chapzlock.core.render.TextureRegistry;
import org.chapzlock.core.scene.SceneManager;
import org.chapzlock.core.tween.TweenManager;
import org.chapzlock.core.window.Window;

import lombok.Getter;

/**
 * Immutable container for all engine subsystems.
 *
 * <p>Created by {@link Application} at startup and passed to systems via {@code onInit()}.
 * Contains: {@code ComponentRegistry}, {@code EventBus}, {@code Window}, GPU registries
 * ({@code MeshRegistry}, {@code TextureRegistry}, {@code ShaderRegistry}),
 * {@code Time}, {@code TweenManager}, {@code SceneManager}, {@code InputMap}.
 *
 * <p>{@code CameraSystem} is set after construction because it is created as an engine system
 * (not a subsystem) and must be available to other systems during their {@code onInit()}.
 *
 * <p>For unit tests, use {@link #testEngine()} to create a minimal context with no GPU subsystems.
 */
@Getter
public class Engine implements Disposable {
    private final ComponentRegistry registry;
    private final EventBus eventBus;
    private final MeshRegistry meshRegistry;
    private final TextureRegistry textureRegistry;
    private final ShaderRegistry shaderRegistry;
    private final Window window;
    private final Time time;
    private final TweenManager tweenManager;
    private final GltfAssetRegistry gltfAssetRegistry;
    private final List<GameSystem> engineSystems;
    /**
     * Set after construction due to circular dependency: SceneManager needs
     * Engine in its constructor, but Engine holds SceneManager.
     * This is intentional — set once during Application.run() and never changed after.
     */
    private SceneManager sceneManager;
    private InputMap activeInputMap;
    private Physics physics;
    private EntitySystem entitySystem;
    /**
     * Set after construction by Application.createEngineSystems(). Available to all
     * engine systems during their onInit() call.
     */
    private CameraSystem cameraSystem;

    public Engine(ComponentRegistry registry,
           EventBus eventBus,
           MeshRegistry meshRegistry,
           TextureRegistry textureRegistry,
           ShaderRegistry shaderRegistry,
           Window window,
           Time time,
           TweenManager tweenManager,
           GltfAssetRegistry gltfAssetRegistry,
           List<GameSystem> engineSystems) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.meshRegistry = meshRegistry;
        this.textureRegistry = textureRegistry;
        this.shaderRegistry = shaderRegistry;
        this.window = window;
        this.time = time;
        this.tweenManager = tweenManager;
        this.gltfAssetRegistry = gltfAssetRegistry;
        this.engineSystems = engineSystems;
    }

    /**
     * Creates a minimal engine for unit tests (no GPU systems).
     *
     * @return a test engine with {@code ComponentRegistry}, {@code EventBus}, {@code Time}, and {@code TweenManager}
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
            new TweenManager(),
            new GltfAssetRegistry(),
            List.of()
        );
        EntitySystem entitySystem = new EntitySystem();
        entitySystem.onInit(engine);
        engine.setEntitySystem(entitySystem);
        return engine;
    }

    @Override
    public void dispose() {
        if (shaderRegistry != null) shaderRegistry.dispose();
        if (textureRegistry != null) textureRegistry.dispose();
        if (meshRegistry != null) meshRegistry.dispose();
        if (eventBus != null) eventBus.clearAllListeners();
    }

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public void setActiveInputMap(InputMap activeInputMap) {
        this.activeInputMap = activeInputMap;
    }

    public void setPhysics(Physics physics) {
        this.physics = physics;
    }

    public void setEntitySystem(EntitySystem entitySystem) {
        this.entitySystem = entitySystem;
    }

    public void setCameraSystem(CameraSystem cameraSystem) {
        this.cameraSystem = cameraSystem;
    }
}
