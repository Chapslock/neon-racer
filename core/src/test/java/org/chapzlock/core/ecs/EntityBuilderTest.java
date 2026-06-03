package org.chapzlock.core.ecs;

import org.chapzlock.core.component.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityBuilderTest {

    record TestTag() implements Tag {}
    record AnotherTag() implements Tag {}
    record TestComponent(int value) implements Component {}

    @Test
    void componentAddsToList() {
        TestComponent c = new TestComponent(42);

        EntityBuilder builder = new EntityBuilder("test")
            .component(c);

        assertEquals(1, builder.getComponents().size());
        assertTrue(builder.getComponents().contains(c));
    }

    @Test
    void varArgsAddsMultiple() {
        TestComponent c1 = new TestComponent(1);
        TestComponent c2 = new TestComponent(2);
        TestComponent c3 = new TestComponent(3);

        EntityBuilder builder = new EntityBuilder("multi")
            .component(c1, c2, c3);

        assertEquals(3, builder.getComponents().size());
        assertTrue(builder.getComponents().contains(c1));
        assertTrue(builder.getComponents().contains(c2));
        assertTrue(builder.getComponents().contains(c3));
    }

    @Test
    void tagAccumulates() {
        EntityBuilder builder = new EntityBuilder("tagged")
            .tag(new TestTag())
            .tag(new AnotherTag());

        assertEquals(2, builder.getTags().size());
        assertInstanceOf(TestTag.class, builder.getTags().get(0));
        assertInstanceOf(AnotherTag.class, builder.getTags().get(1));
    }

    @Test
    void getNameWorks() {
        EntityBuilder builder = new EntityBuilder("player");
        assertEquals("player", builder.getName());
    }

    @Test
    void nullNameAllowed() {
        EntityBuilder builder = new EntityBuilder(null);
        assertNull(builder.getName());
        assertTrue(builder.getComponents().isEmpty());
        assertTrue(builder.getTags().isEmpty());
    }

    @Test
    void transformDelegatesToComponent() {
        Transform transform = Transform.at(1, 2, 3);
        DirectionalLight light = DirectionalLight.builder()
            .direction(new org.joml.Vector3f(0, -1, 0))
            .build();
        PointLight pointLight = PointLight.builder().build();
        Fog fog = Fog.builder().density(0.01f).gradient(2.0f).build();
        AmbientLight ambient = AmbientLight.builder().strength(0.5f).build();
        Camera camera = new Camera(new org.joml.Vector3f(0, 5, 10));

        EntityBuilder builder = new EntityBuilder("full")
            .transform(transform)
            .component(light)
            .component(pointLight)
            .component(fog)
            .component(ambient)
            .component(camera)
            .component(new Sky());

        assertTrue(builder.getComponents().contains(transform));
        assertTrue(builder.getComponents().contains(light));
        assertTrue(builder.getComponents().contains(pointLight));
        assertTrue(builder.getComponents().contains(fog));
        assertTrue(builder.getComponents().contains(ambient));
        assertTrue(builder.getComponents().contains(camera));
        assertTrue(builder.getComponents().stream().anyMatch(c -> c instanceof Sky));
    }

    @Test
    void fluentChainingWorksForAllMethods() {
        // Verify the builder returns itself for all methods
        EntityBuilder builder = new EntityBuilder("full")
            .transform(Transform.at(0, 0, 0))
            .tag(new TestTag())
            .component(new TestComponent(42))
            .component(PhysicsBody.dynamic(1f));

        assertNotNull(builder);
        assertEquals("full", builder.getName());
        assertEquals(3, builder.getComponents().size());
        assertEquals(1, builder.getTags().size());
    }
}
