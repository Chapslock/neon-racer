package org.chapzlock.core.scene;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityBlueprintBuilderTest {

    record TestTag() implements Tag {}

    @Test
    void transformBuildsCorrectComponent() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test");
        builder.transform(1, 2, 3);
        EntityBlueprint blueprint = builder.build();

        EngineContext ctx = EngineContext.testContext();
        Component[] components = blueprint.getFactories().get(0).create(ctx);

        assertInstanceOf(Transform.class, components[0]);
        Transform t = (Transform) components[0];
        assertEquals(1f, t.getPosition().x);
        assertEquals(2f, t.getPosition().y);
        assertEquals(3f, t.getPosition().z);
    }

    @Test
    void tagBuildsCorrectComponent() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test");
        builder.tag(new TestTag());
        EntityBlueprint blueprint = builder.build();

        EngineContext ctx = EngineContext.testContext();
        Component[] components = blueprint.getFactories().get(0).create(ctx);

        assertInstanceOf(TestTag.class, components[0]);
    }

    @Test
    void componentEscapeHatchAddsArbitraryComponent() {
        Transform transform = new Transform();
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test");
        builder.component(transform);
        EntityBlueprint blueprint = builder.build();

        EngineContext ctx = EngineContext.testContext();
        Component[] components = blueprint.getFactories().get(0).create(ctx);

        assertSame(transform, components[0]);
    }

    @Test
    void builderNameIsPreserved() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("my-entity");
        EntityBlueprint blueprint = builder.build();
        assertEquals("my-entity", blueprint.getName());
    }

    @Test
    void multipleCallsAccumulateFactories() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test");
        builder.transform(0, 0, 0);
        builder.tag(new TestTag());
        EntityBlueprint blueprint = builder.build();

        assertEquals(2, blueprint.getFactories().size());
    }

    @Test
    void meshWithNullSystemReturnsEmpty() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test");
        builder.mesh("obj/car.obj");
        EntityBlueprint blueprint = builder.build();

        EngineContext ctx = EngineContext.testContext();
        Component[] components = blueprint.getFactories().get(0).create(ctx);

        assertEquals(0, components.length);
    }

    @Test
    void textureWithNullSystemReturnsEmpty() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test");
        builder.texture("textures/car.png");
        EntityBlueprint blueprint = builder.build();

        EngineContext ctx = EngineContext.testContext();
        Component[] components = blueprint.getFactories().get(0).create(ctx);

        assertEquals(0, components.length);
    }

    @Test
    void modelWithNullSystemsReturnsEmpty() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test");
        builder.model("obj/car.obj", "textures/car.png");
        EntityBlueprint blueprint = builder.build();

        EngineContext ctx = EngineContext.testContext();
        Component[] components = blueprint.getFactories().get(0).create(ctx);

        assertEquals(0, components.length);
    }
}
