package org.chapzlock.core.application;

import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.render.CameraSystem;
import org.chapzlock.core.render.MaterialSystem;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.scene.SceneManager;
import org.chapzlock.core.tween.TweenManager;
import org.chapzlock.core.window.Window;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class EngineContext implements Disposable {
    private final ComponentRegistry registry;
    private final EventBus eventBus;
    private final MeshSystem meshSystem;
    private final TextureSystem textureSystem;
    private final ShaderSystem shaderSystem;
    private final MaterialSystem materialSystem;
    private final CameraSystem cameraSystem;
    private final Window window;
    private final Time time;
    private final TweenManager tweenManager;
    @Setter
    private SceneManager sceneManager;
    @Setter
    private InputMap activeInputMap;

    public static EngineContext testContext() {
        EventBus eventBus = new EventBus();
        return EngineContext.builder()
            .registry(new ComponentRegistry(eventBus))
            .eventBus(eventBus)
            .time(new Time())
            .tweenManager(new TweenManager())
            .build();
    }

    @Override
    public void dispose() {
        if (shaderSystem != null) shaderSystem.dispose();
        if (textureSystem != null) textureSystem.dispose();
        if (meshSystem != null) meshSystem.dispose();
        if (eventBus != null) eventBus.clearAllListeners();
    }
}
