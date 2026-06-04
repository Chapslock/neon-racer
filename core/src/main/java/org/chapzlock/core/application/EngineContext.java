package org.chapzlock.core.application;

import org.chapzlock.core.asset.gltf.GltfAssetRegistry;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntitySystem;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.physics.Physics;
import org.chapzlock.core.render.CameraSystem;
import org.chapzlock.core.render.MeshRegistry;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.scene.SceneManager;
import org.chapzlock.core.tween.TweenManager;
import org.chapzlock.core.window.Window;

import lombok.Builder;
import lombok.Getter;

/**
 * Container for all engine subsystems.
 *
 * <p>Created by {@link Application} at startup and passed to systems via {@code onInit()}.
 * Contains: {@code ComponentRegistry}, {@code EventBus}, {@code Window}, GPU systems
 * ({@code MeshRegistry}, {@code TextureSystem}, {@code ShaderSystem}),
 * {@code CameraSystem}, {@code Time}, {@code TweenManager}, {@code SceneManager}, {@code InputMap}.
 *
 * <p>For unit tests, use {@link #testContext()} to create a minimal context with no GPU subsystems.
 */
@Getter
@Builder
public class EngineContext implements Disposable {
    private final ComponentRegistry registry;
    private final EventBus eventBus;
    private final MeshRegistry meshRegistry;
    private final TextureSystem textureSystem;
    private final ShaderSystem shaderSystem;
    private final CameraSystem cameraSystem;
    private final Window window;
    private final Time time;
    private final TweenManager tweenManager;
    private final GltfAssetRegistry gltfAssetRegistry;
    /**
     * Set after construction due to circular dependency: SceneManager needs
     * EngineContext in its constructor, but EngineContext holds SceneManager.
     * This is intentional — set once during Application.run() and never changed after.
     */
    private SceneManager sceneManager;
    private InputMap activeInputMap;
    private Physics physics;
    private EntitySystem entitySystem;

    /**
     * Creates a minimal context for unit tests (no GPU systems).
     *
     * @return a test context with {@code ComponentRegistry}, {@code EventBus}, {@code Time}, and {@code TweenManager}
     */
    public static EngineContext testContext() {
        EventBus eventBus = new EventBus();
        EngineContext ctx = EngineContext.builder()
            .registry(new ComponentRegistry(eventBus))
            .eventBus(eventBus)
            .time(new Time())
            .tweenManager(new TweenManager())
            .gltfAssetRegistry(new GltfAssetRegistry())
            .build();
        EntitySystem entitySystem = new EntitySystem();
        entitySystem.onInit(ctx);
        ctx.setEntitySystem(entitySystem);
        return ctx;
    }

    @Override
    public void dispose() {
        if (shaderSystem != null) shaderSystem.dispose();
        if (textureSystem != null) textureSystem.dispose();
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
}
