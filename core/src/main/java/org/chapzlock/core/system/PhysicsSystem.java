package org.chapzlock.core.system;

import org.chapzlock.core.application.System;
import org.chapzlock.core.registry.ComponentRegistry;

public class PhysicsSystem implements System {

    private final ComponentRegistry registry = ComponentRegistry.instance();

    @Override
    public void onUpdate(float deltaTime) {
        System.super.onUpdate(deltaTime);
    }

    @Override
    public void onInit() {
        System.super.onInit();
    }

    @Override
    public void onDestroy() {
        System.super.onDestroy();
    }
}
