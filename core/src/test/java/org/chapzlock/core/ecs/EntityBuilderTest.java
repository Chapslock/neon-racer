package org.chapzlock.core.ecs;

import org.chapzlock.core.component.*;
import org.chapzlock.core.scene.EntityBlueprint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityBuilderTest {

    record TestTag() implements Tag {}
    record AnotherTag() implements Tag {}
    record TestComponent(int value) implements Component {}

    @Test
    void build_producesEntityBlueprintWithAllFields() {
        Transform transform = Transform.at(1, 2, 3);
        PhysicsDescriptor physics = PhysicsDescriptor.dynamic(5f);

        EntityBlueprint bp = new EntityBuilder("player")
            .transform(transform)
            .physics(physics)
            .tag(new TestTag())
            .gltfModel("glb/car.glb")
            .build();

        assertEquals("player", bp.getName());
        assertTrue(bp.getComponents().contains(transform));
        assertSame(physics, bp.getPhysicsDescriptor());
        assertEquals(1, bp.getTags().size());
        assertInstanceOf(TestTag.class, bp.getTags().get(0));
        assertEquals("glb/car.glb", bp.getGltfModelPath());
    }

    @Test
    void build_withNullName() {
        EntityBlueprint bp = new EntityBuilder(null).build();

        assertNull(bp.getName());
        assertTrue(bp.getComponents().isEmpty());
        assertTrue(bp.getTags().isEmpty());
    }

    @Test
    void build_multipleComponents() {
        TestComponent c1 = new TestComponent(1);
        TestComponent c2 = new TestComponent(2);
        TestComponent c3 = new TestComponent(3);

        EntityBlueprint bp = new EntityBuilder("multi")
            .component(c1, c2, c3)
            .build();

        assertEquals(3, bp.getComponents().size());
        assertTrue(bp.getComponents().contains(c1));
        assertTrue(bp.getComponents().contains(c2));
        assertTrue(bp.getComponents().contains(c3));
    }

    @Test
    void spriteDescriptorIsStored() {
        SpriteDescriptor desc = SpriteDescriptor.of("textures/hud.png");

        EntityBlueprint bp = new EntityBuilder("sprite-entity")
            .sprite(desc)
            .build();

        assertSame(desc, bp.getSpriteDescriptor());
    }

    @Test
    void tagsAccumulateAcrossMultipleCalls() {
        EntityBlueprint bp = new EntityBuilder("tagged")
            .tag(new TestTag())
            .tag(new AnotherTag())
            .build();

        assertEquals(2, bp.getTags().size());
    }

    @Test
    void directionalLightAddsToComponents() {
        DirectionalLight light = DirectionalLight.builder()
            .direction(new org.joml.Vector3f(0, -1, 0))
            .build();

        EntityBlueprint bp = new EntityBuilder("sun")
            .directionalLight(light)
            .build();

        assertTrue(bp.getComponents().contains(light));
    }

    @Test
    void pointLightAddsToComponents() {
        PointLight light = PointLight.builder().build();

        EntityBlueprint bp = new EntityBuilder("torch")
            .pointLight(light)
            .build();

        assertTrue(bp.getComponents().contains(light));
    }

    @Test
    void fogAddsToComponents() {
        Fog fog = Fog.builder().density(0.01f).gradient(2.0f).build();

        EntityBlueprint bp = new EntityBuilder("fog-entity")
            .fog(fog)
            .build();

        assertTrue(bp.getComponents().contains(fog));
    }

    @Test
    void ambientAddsToComponents() {
        AmbientLight ambient = AmbientLight.builder().strength(0.5f).build();

        EntityBlueprint bp = new EntityBuilder("ambient-entity")
            .ambient(ambient)
            .build();

        assertTrue(bp.getComponents().contains(ambient));
    }

    @Test
    void cameraAddsToComponents() {
        Camera camera = new Camera(new org.joml.Vector3f(0, 5, 10));

        EntityBlueprint bp = new EntityBuilder("cam")
            .camera(camera)
            .build();

        assertTrue(bp.getComponents().contains(camera));
    }

    @Test
    void skyAddsSkyComponent() {
        EntityBlueprint bp = new EntityBuilder("sky-entity")
            .sky()
            .build();

        assertEquals(1, bp.getComponents().size());
        assertInstanceOf(Sky.class, bp.getComponents().get(0));
    }

    @Test
    void fluentChainingWorksForAllMethods() {
        // Verify the builder returns itself for all methods
        EntityBuilder builder = new EntityBuilder("full")
            .transform(Transform.at(0, 0, 0))
            .sprite(SpriteDescriptor.of("tex.png"))
            .physics(PhysicsDescriptor.dynamic(1f))
            .tag(new TestTag())
            .component(new TestComponent(42))
            .gltfModel("glb/model.glb");

        EntityBlueprint bp = builder.build();
        assertNotNull(bp);
        assertEquals("full", bp.getName());
    }
}
