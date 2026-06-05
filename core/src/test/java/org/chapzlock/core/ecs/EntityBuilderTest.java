package org.chapzlock.core.ecs;

import org.chapzlock.core.component.Transform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityBuilderTest {

    @Test
    void create_namedBuilder() {
        EntityBuilder builder = EntityBuilder.create("player");
        assertEquals("player", builder.getName());
    }

    @Test
    void create_anonymousBuilder() {
        EntityBuilder builder = EntityBuilder.create();
        assertNull(builder.getName());
    }

    @Test
    void component_accumulates() {
        EntityBuilder builder = EntityBuilder.create("test")
            .transform(Transform.at(1, 2, 3));
        assertEquals(1, builder.getComponents().size());
        assertInstanceOf(Transform.class, builder.getComponents().get(0));
    }

    @Test
    void children_storedAsNestedBuilders() {
        EntityBuilder builder = EntityBuilder.create("parent")
            .transform(Transform.at(0, 0, 0))
            .child("a", c -> c.transform(Transform.at(1, 0, 0)))
            .child("b", c -> c.transform(Transform.at(2, 0, 0)));

        assertEquals(2, builder.getChildren().size());
        assertEquals("a", builder.getChildren().get(0).getName());
        assertEquals("b", builder.getChildren().get(1).getName());
    }

    @Test
    void noInstantiateMethod_exists() {
        assertThrows(NoSuchMethodException.class,
            () -> EntityBuilder.class.getMethod("instantiate"));
    }
}
