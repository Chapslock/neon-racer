package org.chapzlock.app.systems;

import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.joml.Vector3f;

public class CharacterRotateSystem implements GameSystem {
    private ComponentRegistry registry;
    private final Vector3f tempEuler = new Vector3f();

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        registry.forEach(PlayerTag.class, Transform.class, (entityId, tag, transform) -> {
            float speed = 10 * deltaTime;
            transform.getRotationEuler(tempEuler);
            tempEuler.x += speed;
            tempEuler.y += speed;
            tempEuler.z += speed;
            transform.setRotationEuler(tempEuler.x, tempEuler.y, tempEuler.z);
        });
    }
}
