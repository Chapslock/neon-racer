package org.chapzlock.core.scene;

import org.chapzlock.core.component.PhysicsDescriptor;
import org.chapzlock.core.component.SpriteDescriptor;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.EntityBuilder;
import org.chapzlock.core.ecs.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests verifying that EntityBuilder produces correct EntityBlueprint instances
 * for use in scene declarations.
 */
class EntityBlueprintBuilderTest {

    record TestTag() implements Tag {}
    record TestComponent() implements Component {}

    @Test
    void builderNameIsPreserved() {
        EntityBlueprint bp = new EntityBuilder("my-entity").build();
        assertEquals("my-entity", bp.getName());
    }

    @Test
    void transformIsStoredInComponents() {
        Transform t = Transform.at(1, 2, 3);
        EntityBlueprint bp = new EntityBuilder("test")
            .transform(t)
            .build();
        assertTrue(bp.getComponents().contains(t));
    }

    @Test
    void spriteDescriptorIsStored() {
        SpriteDescriptor desc = SpriteDescriptor.of("sprite.png");
        EntityBlueprint bp = new EntityBuilder("test")
            .sprite(desc)
            .build();
        assertSame(desc, bp.getSpriteDescriptor());
    }

    @Test
    void physicsDescriptorIsStored() {
        PhysicsDescriptor desc = PhysicsDescriptor.dynamic(5f);
        EntityBlueprint bp = new EntityBuilder("test")
            .physics(desc)
            .build();
        assertSame(desc, bp.getPhysicsDescriptor());
    }

    @Test
    void tagsAreAccumulated() {
        EntityBlueprint bp = new EntityBuilder("test")
            .tag(new TestTag())
            .tag(new TestTag())
            .build();
        assertEquals(2, bp.getTags().size());
    }

    @Test
    void componentsAreAccumulated() {
        EntityBlueprint bp = new EntityBuilder("test")
            .component(new TestComponent(), new TestComponent())
            .build();
        assertEquals(2, bp.getComponents().size());
    }

}
