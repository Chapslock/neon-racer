package org.chapzlock.app.systems;

import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;

public class PlayerRotateSystem implements GameSystem {
    private ComponentRegistry registry;

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        registry.forEach(PlayerTag.class, Transform.class, (entityId, tag, transform) -> {
            float speed = 10 * deltaTime;
            transform.getRotationEuler().x += speed;
            transform.getRotationEuler().y += speed;
            transform.getRotationEuler().z += speed;
        });
    }
}
