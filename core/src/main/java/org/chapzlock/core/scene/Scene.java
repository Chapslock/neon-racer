package org.chapzlock.core.scene;

import java.util.List;

import org.chapzlock.core.ecs.GameSystem;

import lombok.Getter;

@Getter
public class Scene {
    private final String name;
    private final List<EntityBlueprint> entities;
    private final List<GameSystem> systems;

    public Scene(String name, List<EntityBlueprint> entities, List<GameSystem> systems) {
        if (name == null) {
            throw new IllegalArgumentException(
                "Scene name cannot be null. Pass a name to identify the scene in logs and error messages.");
        }
        this.name = name;
        this.entities = List.copyOf(entities);
        this.systems = List.copyOf(systems);
    }

    public static SceneBuilder builder(String name) {
        return new SceneBuilder(name);
    }
}
