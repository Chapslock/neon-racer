package org.chapzlock.core.component;

import org.chapzlock.core.physics.CollisionGeometry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicsBodyTest {

    @Test
    void dynamicCreatesBodyWithMassAndDefaults() {
        PhysicsBody body = PhysicsBody.dynamic(2.5f);

        assertEquals(2.5f, body.getMass(), 0.001f);
        assertEquals(0.5f, body.getFriction(), 0.001f);
        assertEquals(0.1f, body.getRestitution(), 0.001f);
        assertEquals(0.05f, body.getLinearDamping(), 0.001f);
        assertEquals(0.85f, body.getAngularDamping(), 0.001f);
        assertNull(body.getGeometry());
        assertEquals(1f, body.getAngularFactor(), 0.001f);
        assertEquals(1, body.getLayer());
        assertEquals(0xFFFFFFFF, body.getCollisionMask());
        assertTrue(body.isDynamic());
        assertFalse(body.isStatic());
    }

    @Test
    void asStaticCreatesBodyWithZeroMass() {
        PhysicsBody body = PhysicsBody.asStatic();

        assertEquals(0f, body.getMass(), 0.001f);
        assertEquals(0.5f, body.getFriction(), 0.001f);
        assertEquals(0.1f, body.getRestitution(), 0.001f);
        assertEquals(0.05f, body.getLinearDamping(), 0.001f);
        assertEquals(0.85f, body.getAngularDamping(), 0.001f);
        assertNull(body.getGeometry());
        assertEquals(1f, body.getAngularFactor(), 0.001f);
        assertEquals(1, body.getLayer());
        assertEquals(0xFFFFFFFF, body.getCollisionMask());
        assertFalse(body.isDynamic());
        assertTrue(body.isStatic());
    }

    @Test
    void shapeReturnsNewInstanceWithGeometry() {
        CollisionGeometry geom = CollisionGeometry.box(1f, 2f, 3f);
        PhysicsBody original = PhysicsBody.dynamic(1f);
        PhysicsBody withShape = original.shape(geom);

        assertNotSame(original, withShape);
        assertNull(original.getGeometry());
        assertSame(geom, withShape.getGeometry());
        assertEquals(1f, withShape.getMass(), 0.001f);
    }

    @Test
    void fullFluentChainingPreservesAllFields() {
        CollisionGeometry geom = CollisionGeometry.capsule(0.5f, 2f);
        int playerLayer = 0b0001;
        int terrainLayer = 0b0010;

        PhysicsBody body = PhysicsBody.dynamic(1.5f)
            .shape(geom)
            .friction(0.7f)
            .restitution(0.3f)
            .linearDamping(0.1f)
            .angularDamping(0.9f)
            .angularFactor(0.5f)
            .layer(playerLayer)
            .collidesWith(terrainLayer);

        assertEquals(1.5f, body.getMass(), 0.001f);
        assertSame(geom, body.getGeometry());
        assertEquals(0.7f, body.getFriction(), 0.001f);
        assertEquals(0.3f, body.getRestitution(), 0.001f);
        assertEquals(0.1f, body.getLinearDamping(), 0.001f);
        assertEquals(0.9f, body.getAngularDamping(), 0.001f);
        assertEquals(0.5f, body.getAngularFactor(), 0.001f);
        assertEquals(playerLayer, body.getLayer());
        assertEquals(terrainLayer, body.getCollisionMask());
    }

    @Test
    void lockRotationSetsAngularFactorToZero() {
        PhysicsBody body = PhysicsBody.dynamic(1f).lockRotation();

        assertEquals(0f, body.getAngularFactor(), 0.001f);
        assertEquals(1f, body.getMass(), 0.001f);
    }
}
