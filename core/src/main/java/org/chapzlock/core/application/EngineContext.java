package org.chapzlock.core.application;

import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.render.CameraSystem;
import org.chapzlock.core.render.MaterialRendererRegistry;
import org.chapzlock.core.render.MaterialSystem;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.scene.SceneManager;
import org.chapzlock.core.tween.TweenManager;
import org.chapzlock.core.window.Window;

import lombok.Getter;
import lombok.Setter;

@Getter
public class EngineContext implements Disposable {
    private final ComponentRegistry registry;
    private final EventBus eventBus;
    private final MeshSystem meshSystem;
    private final TextureSystem textureSystem;
    private final ShaderSystem shaderSystem;
    private final MaterialSystem materialSystem;
    private final MaterialRendererRegistry materialRendererRegistry;
    private final CameraSystem cameraSystem;
    private final Window window;
    private final Time time;
    private final TweenManager tweenManager;
    @Setter
    private SceneManager sceneManager;
    @Setter
    private InputMap activeInputMap;

    public EngineContext(
            ComponentRegistry registry,
            EventBus eventBus,
            MeshSystem meshSystem,
            TextureSystem textureSystem,
            ShaderSystem shaderSystem,
            MaterialSystem materialSystem,
            MaterialRendererRegistry materialRendererRegistry,
            CameraSystem cameraSystem,
            Window window,
            Time time,
            TweenManager tweenManager) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.meshSystem = meshSystem;
        this.textureSystem = textureSystem;
        this.shaderSystem = shaderSystem;
        this.materialSystem = materialSystem;
        this.materialRendererRegistry = materialRendererRegistry;
        this.cameraSystem = cameraSystem;
        this.window = window;
        this.time = time;
        this.tweenManager = tweenManager;
    }

    public static EngineContext testContext() {
        EventBus eventBus = new EventBus();
        return new EngineContext(
            new ComponentRegistry(eventBus),
            eventBus,
            null, null, null, null, null, null, null,
            new Time(),
            new TweenManager()
        );
    }

    @Override
    public void dispose() {
        if (shaderSystem != null) shaderSystem.dispose();
        if (textureSystem != null) textureSystem.dispose();
        if (meshSystem != null) meshSystem.dispose();
        if (eventBus != null) eventBus.clearAllListeners();
    }
}
