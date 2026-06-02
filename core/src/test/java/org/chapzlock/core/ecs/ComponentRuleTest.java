package org.chapzlock.core.ecs;

import java.util.List;

import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.physics.PhysicsSpecs;
import org.junit.jupiter.api.Test;

import com.bulletphysics.collision.shapes.BoxShape;

import javax.vecmath.Vector3f;

import static org.junit.jupiter.api.Assertions.*;

class ComponentRuleTest {

    record FakeComponent() implements Component {}

    @Test
    void ruleNotViolatedWhenBothPresent() {
        ComponentRule rule = ComponentRule.requires(Mesh.class, Transform.class,
            "Mesh requires Transform");
        List<Component> components = List.of(
            Mesh.builder().build(),
            Transform.at(0, 0, 0)
        );
        assertFalse(rule.isViolated(components));
    }

    @Test
    void ruleNotViolatedWhenComponentAbsent() {
        ComponentRule rule = ComponentRule.requires(Mesh.class, Transform.class,
            "Mesh requires Transform");
        List<Component> components = List.of(Transform.at(0, 0, 0));
        assertFalse(rule.isViolated(components));
    }

    @Test
    void ruleViolatedWhenComponentPresentButRequirementMissing() {
        ComponentRule rule = ComponentRule.requires(Mesh.class, Transform.class,
            "Mesh requires Transform");
        List<Component> components = List.of(Mesh.builder().build());
        assertTrue(rule.isViolated(components));
    }

    @Test
    void messageIsAccessible() {
        ComponentRule rule = ComponentRule.requires(Mesh.class, Transform.class,
            "Mesh requires Transform — entity won't be positioned");
        assertEquals("Mesh requires Transform — entity won't be positioned", rule.message());
    }

    @Test
    void ruleWorksWithTagsInComponentList() {
        ComponentRule rule = ComponentRule.requires(PhysicsBody.class, Transform.class,
            "PhysicsBody requires Transform");
        PhysicsBody physicsBody = new PhysicsBody(
            new BoxShape(new Vector3f(1, 1, 1)),
            PhysicsSpecs.builder().build()
        );
        List<Component> components = List.of(
            physicsBody,
            new FakeComponent()
        );
        assertTrue(rule.isViolated(components));
    }
}
