package org.chapzlock.core.physics;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollisionGeometryTest {

    @Test
    void boxCreatesWithHalfExtents() {
        var box = CollisionGeometry.box(1f, 2f, 3f);
        assertInstanceOf(CollisionGeometry.Box.class, box);
        var b = (CollisionGeometry.Box) box;
        assertEquals(1f, b.halfExtents().x);
        assertEquals(2f, b.halfExtents().y);
        assertEquals(3f, b.halfExtents().z);
    }

    @Test
    void boxCreatesFromVector() {
        var box = CollisionGeometry.box(new Vector3f(0.5f, 1f, 0.5f));
        assertInstanceOf(CollisionGeometry.Box.class, box);
    }

    @Test
    void sphereCreatesWithRadius() {
        var sphere = CollisionGeometry.sphere(2.5f);
        assertInstanceOf(CollisionGeometry.Sphere.class, sphere);
        assertEquals(2.5f, ((CollisionGeometry.Sphere) sphere).radius());
    }

    @Test
    void capsuleCreatesWithRadiusAndHeight() {
        var capsule = CollisionGeometry.capsule(0.4f, 1.8f);
        assertInstanceOf(CollisionGeometry.Capsule.class, capsule);
        var c = (CollisionGeometry.Capsule) capsule;
        assertEquals(0.4f, c.radius());
        assertEquals(1.8f, c.height());
    }

    @Test
    void convexHullDefaultsTo256MaxVertices() {
        var hull = CollisionGeometry.convexHull();
        assertInstanceOf(CollisionGeometry.ConvexHull.class, hull);
        assertEquals(256, ((CollisionGeometry.ConvexHull) hull).maxVertices());
    }

    @Test
    void convexHullWithCustomMaxVertices() {
        var hull = CollisionGeometry.convexHull(64);
        assertEquals(64, ((CollisionGeometry.ConvexHull) hull).maxVertices());
    }

    @Test
    void triangleMeshCreates() {
        var mesh = CollisionGeometry.triangleMesh();
        assertInstanceOf(CollisionGeometry.TriangleMesh.class, mesh);
    }

    @Test
    void compoundCreatesWithChildren() {
        var compound = CollisionGeometry.compound(
            new CollisionGeometry.Positioned(CollisionGeometry.sphere(0.3f), new Vector3f(0, 1, 0)),
            new CollisionGeometry.Positioned(CollisionGeometry.capsule(0.4f, 1.5f), new Vector3f(0, 0, 0))
        );
        assertInstanceOf(CollisionGeometry.Compound.class, compound);
        assertEquals(2, ((CollisionGeometry.Compound) compound).children().size());
    }

    @Test
    void sphereRejectsNegativeRadius() {
        assertThrows(IllegalArgumentException.class, () -> CollisionGeometry.sphere(-1f));
    }

    @Test
    void capsuleRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> CollisionGeometry.capsule(-0.5f, 1f));
        assertThrows(IllegalArgumentException.class, () -> CollisionGeometry.capsule(0.5f, -1f));
    }

    @Test
    void convexHullRejectsZeroOrNegativeMaxVertices() {
        assertThrows(IllegalArgumentException.class, () -> CollisionGeometry.convexHull(0));
        assertThrows(IllegalArgumentException.class, () -> CollisionGeometry.convexHull(-5));
    }

    @Test
    void offsetWrapsShapeWithVector() {
        var capsule = CollisionGeometry.capsule(0.4f, 1.8f).offset(0, 0.9f, 0);
        assertInstanceOf(CollisionGeometry.Offset.class, capsule);
        var o = (CollisionGeometry.Offset) capsule;
        assertInstanceOf(CollisionGeometry.Capsule.class, o.inner());
        assertEquals(0f, o.offset().x, 0.001f);
        assertEquals(0.9f, o.offset().y, 0.001f);
        assertEquals(0f, o.offset().z, 0.001f);
    }

    @Test
    void offsetOnAlreadyOffsetReplacesOffset() {
        var shape = CollisionGeometry.box(1, 1, 1).offset(1, 2, 3).offset(4, 5, 6);
        assertInstanceOf(CollisionGeometry.Offset.class, shape);
        var o = (CollisionGeometry.Offset) shape;
        assertInstanceOf(CollisionGeometry.Box.class, o.inner());
        assertEquals(4f, o.offset().x, 0.001f);
        assertEquals(5f, o.offset().y, 0.001f);
        assertEquals(6f, o.offset().z, 0.001f);
    }

    @Test
    void offsetWorksOnAllShapeTypes() {
        assertInstanceOf(CollisionGeometry.Offset.class, CollisionGeometry.sphere(1f).offset(0, 1, 0));
        assertInstanceOf(CollisionGeometry.Offset.class, CollisionGeometry.box(1, 1, 1).offset(0, 1, 0));
        assertInstanceOf(CollisionGeometry.Offset.class, CollisionGeometry.capsule(0.5f, 2f).offset(0, 1, 0));
        assertInstanceOf(CollisionGeometry.Offset.class, CollisionGeometry.convexHull().offset(0, 1, 0));
        assertInstanceOf(CollisionGeometry.Offset.class, CollisionGeometry.triangleMesh().offset(0, 1, 0));
    }
}
