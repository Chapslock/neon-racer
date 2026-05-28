package org.chapzlock.core.application;

import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.render.MaterialRendererRegistry;
import org.chapzlock.core.render.MaterialSystem;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.window.Window;

import lombok.Getter;

@Getter
public class EngineContext implements Disposable {
    private final ComponentRegistry registry;
    private final EventBus eventBus;
    private final MeshSystem meshSystem;
    private final TextureSystem textureSystem;
    private final ShaderSystem shaderSystem;
    private final MaterialSystem materialSystem;
    private final MaterialRendererRegistry materialRendererRegistry;
    private final Window window;

    public EngineContext(
            ComponentRegistry registry,
            EventBus eventBus,
            MeshSystem meshSystem,
            TextureSystem textureSystem,
            ShaderSystem shaderSystem,
            MaterialSystem materialSystem,
            MaterialRendererRegistry materialRendererRegistry,
            Window window) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.meshSystem = meshSystem;
        this.textureSystem = textureSystem;
        this.shaderSystem = shaderSystem;
        this.materialSystem = materialSystem;
        this.materialRendererRegistry = materialRendererRegistry;
        this.window = window;
    }

    public static EngineContext testContext() {
        EventBus eventBus = new EventBus();
        return new EngineContext(
            new ComponentRegistry(eventBus),
            eventBus,
            null, null, null, null, null, null
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
