package org.chapzlock.core.system;

import org.chapzlock.core.registry.ComponentRegistry;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PhysicsSystem implements System {

    private final ComponentRegistry registry;

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
