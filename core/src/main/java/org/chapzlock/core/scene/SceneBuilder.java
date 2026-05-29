package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.GameSystem;
import org.joml.Vector3f;

public class SceneBuilder {

    private final String name;
    private final List<EntityBlueprint> entities = new ArrayList<>();
    private final List<GameSystem> systems = new ArrayList<>();

    public SceneBuilder(String name) {
        this.name = name;
    }

    public SceneBuilder entity(String entityName, Consumer<EntityBlueprintBuilder> configurator) {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder(entityName);
        configurator.accept(builder);
        entities.add(builder.build());
        return this;
    }

    public SceneBuilder pointLight(float x, float y, float z) {
        return pointLight(x, y, z, Color.WHITE);
    }

    public SceneBuilder pointLight(float x, float y, float z, Color color) {
        entities.add(new EntityBlueprint("point-light", List.of(
            ctx -> new Component[]{ new PointLight(new Vector3f(x, y, z), color) }
        )));
        return this;
    }

    public SceneBuilder camera(float x, float y, float z) {
        entities.add(new EntityBlueprint("camera", List.of(
            ctx -> new Component[]{ new Camera(new Vector3f(x, y, z)) }
        )));
        return this;
    }

    public SceneBuilder sky() {
        entities.add(new EntityBlueprint("sky", List.of(
            ctx -> new Component[]{ new Sky() }
        )));
        return this;
    }

    public SceneBuilder systems(GameSystem... gameSystems) {
        systems.addAll(List.of(gameSystems));
        return this;
    }

    public Scene build() {
        return new Scene(name, entities, systems);
    }
}
