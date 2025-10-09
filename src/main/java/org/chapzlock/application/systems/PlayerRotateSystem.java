package org.chapzlock.application.systems;

import org.chapzlock.application.tags.PlayerTag;
import org.chapzlock.core.component.TransformComponent;
import org.chapzlock.core.component.orchestration.ComponentRegistry;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.system.System;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerRotateSystem implements System {
    private final ComponentRegistry registry;

    @Override
    public void onUpdate(float deltaTime) {
        for (EntityView e : registry.view(PlayerTag.class, TransformComponent.class)) {
            var transform = e.get(TransformComponent.class);

            transform.getRotation().x += 1;
            transform.getRotation().y += 1;
            transform.getRotation().z += 1;
        }
    }
}
