package org.chapzlock.core.scene;

import org.chapzlock.core.component.ModelDescriptor;
import org.chapzlock.core.component.PhysicsDescriptor;
import org.chapzlock.core.component.SpriteDescriptor;
import org.chapzlock.core.component.TerrainDescriptor;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityBlueprintBuilderTest {

    record TestTag() implements Tag {}
    record TestComponent() implements Component {}

    @Test
    void builderNameIsPreserved() {
        EntityBlueprint bp = new EntityBlueprintBuilder("my-entity").build();
        assertEquals("my-entity", bp.getName());
    }

    @Test
    void transformIsStored() {
        Transform t = Transform.at(1, 2, 3);
        EntityBlueprint bp = new EntityBlueprintBuilder("test")
            .transform(t)
            .build();
        assertSame(t, bp.getTransform());
    }

    @Test
    void modelDescriptorIsStored() {
        ModelDescriptor desc = ModelDescriptor.of("mesh.obj", "tex.png");
        EntityBlueprint bp = new EntityBlueprintBuilder("test")
            .model(desc)
            .build();
        assertSame(desc, bp.getModelDescriptor());
    }

    @Test
    void terrainDescriptorIsStored() {
        TerrainDescriptor desc = TerrainDescriptor.builder()
            .heightmap("hm.png").maxHeight(40).build();
        EntityBlueprint bp = new EntityBlueprintBuilder("test")
            .terrain(desc)
            .build();
        assertSame(desc, bp.getTerrainDescriptor());
    }

    @Test
    void spriteDescriptorIsStored() {
        SpriteDescriptor desc = SpriteDescriptor.of("sprite.png");
        EntityBlueprint bp = new EntityBlueprintBuilder("test")
            .sprite(desc)
            .build();
        assertSame(desc, bp.getSpriteDescriptor());
    }

    @Test
    void physicsDescriptorIsStored() {
        PhysicsDescriptor desc = PhysicsDescriptor.dynamic(5f);
        EntityBlueprint bp = new EntityBlueprintBuilder("test")
            .model(ModelDescriptor.of("m.obj", "t.png"))
            .physics(desc)
            .build();
        assertSame(desc, bp.getPhysicsDescriptor());
    }

    @Test
    void tagsAreAccumulated() {
        EntityBlueprint bp = new EntityBlueprintBuilder("test")
            .tag(new TestTag())
            .tag(new TestTag())
            .build();
        assertEquals(2, bp.getTags().size());
    }

    @Test
    void componentsAreAccumulated() {
        EntityBlueprint bp = new EntityBlueprintBuilder("test")
            .component(new TestComponent(), new TestComponent())
            .build();
        assertEquals(2, bp.getComponents().size());
    }

    @Test
    void modelAndTerrainAreMutuallyExclusive() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test")
            .model(ModelDescriptor.of("m.obj", "t.png"))
            .terrain(TerrainDescriptor.builder().heightmap("hm.png").maxHeight(10).build());

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(ex.getMessage().contains("model() and terrain() are mutually exclusive"));
    }

    @Test
    void modelAndSpriteAreMutuallyExclusive() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test")
            .model(ModelDescriptor.of("m.obj", "t.png"))
            .sprite(SpriteDescriptor.of("s.png"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(ex.getMessage().contains("model() and sprite() are mutually exclusive"));
    }

    @Test
    void terrainAndSpriteAreMutuallyExclusive() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test")
            .terrain(TerrainDescriptor.builder().heightmap("hm.png").maxHeight(10).build())
            .sprite(SpriteDescriptor.of("s.png"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(ex.getMessage().contains("terrain() and sprite() are mutually exclusive"));
    }

    @Test
    void physicsRequiresModelOrTerrain() {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder("test")
            .physics(PhysicsDescriptor.dynamic(1f));

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(ex.getMessage().contains("physics() requires model() or terrain()"));
    }

    @Test
    void physicsWithModelIsValid() {
        EntityBlueprint bp = new EntityBlueprintBuilder("test")
            .model(ModelDescriptor.of("m.obj", "t.png"))
            .physics(PhysicsDescriptor.dynamic(1f))
            .build();
        assertNotNull(bp.getPhysicsDescriptor());
    }

    @Test
    void physicsWithTerrainIsValid() {
        EntityBlueprint bp = new EntityBlueprintBuilder("test")
            .terrain(TerrainDescriptor.builder().heightmap("hm.png").maxHeight(10).build())
            .physics(PhysicsDescriptor.asStatic())
            .build();
        assertNotNull(bp.getPhysicsDescriptor());
    }
}
