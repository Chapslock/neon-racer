package org.chapzlock.app.systems;

import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.registry.ComponentRegistry;
import org.chapzlock.core.system.System;

public class PlayerRotateSystem implements System {
    private final ComponentRegistry registry = ComponentRegistry.instance();

    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView e : registry.view(PlayerTag.class, Transform.class)) {
            var transform = e.get(Transform.class);

            float speed = 10 * deltaTime;
            transform.getRotation().x += speed;
            transform.getRotation().y += speed;
            transform.getRotation().z += speed;
        }
    }
}
