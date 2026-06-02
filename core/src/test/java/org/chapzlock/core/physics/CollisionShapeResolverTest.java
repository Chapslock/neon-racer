package org.chapzlock.core.physics;

import org.chapzlock.core.asset.RawMeshData;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.collision.shapes.SphereShape;

import static org.junit.jupiter.api.Assertions.*;

class CollisionShapeResolverTest {

    private CollisionShapeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CollisionShapeResolver();
    }

    @Test
    void resolvesBox() {
        CollisionShape shape = resolver.resolve(
            CollisionGeometry.box(1f, 2f, 3f), null);
        assertInstanceOf(BoxShape.class, shape);
    }

    @Test
    void resolvesSphere() {
        CollisionShape shape = resolver.resolve(
            CollisionGeometry.sphere(2.5f), null);
        assertInstanceOf(SphereShape.class, shape);
    }

    @Test
    void resolvesCapsule() {
        CollisionShape shape = resolver.resolve(
            CollisionGeometry.capsule(0.4f, 1.8f), null);
        assertInstanceOf(CapsuleShape.class, shape);
    }

    @Test
    void resolvesConvexHull() {
        float[] positions = {
            0, 0, 0,  1, 0, 0,  0, 1, 0,  0, 0, 1,
            1, 1, 0,  1, 0, 1,  0, 1, 1,  1, 1, 1
        };
        RawMeshData meshData = RawMeshData.builder()
            .positions(positions)
            .indices(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 3, 4})
            .build();

        CollisionShape shape = resolver.resolve(
            CollisionGeometry.convexHull(64), meshData);
        assertInstanceOf(ConvexHullShape.class, shape);
    }

    @Test
    void resolvesTriangleMesh() {
        float[] positions = {
            0, 0, 0,  1, 0, 0,  0, 1, 0,
            0, 0, 0,  0, 1, 0,  0, 0, 1
        };
        RawMeshData meshData = RawMeshData.builder()
            .positions(positions)
            .indices(new int[]{0, 1, 2, 3, 4, 5})
            .build();

        CollisionShape shape = resolver.resolve(
            CollisionGeometry.triangleMesh(), meshData);
        assertInstanceOf(BvhTriangleMeshShape.class, shape);
    }

    @Test
    void resolvesCompound() {
        CollisionShape shape = resolver.resolve(
            CollisionGeometry.compound(
                new CollisionGeometry.Positioned(CollisionGeometry.sphere(0.3f), new Vector3f(0, 1, 0)),
                new CollisionGeometry.Positioned(CollisionGeometry.capsule(0.4f, 1.0f), new Vector3f(0, 0, 0))
            ), null);
        assertInstanceOf(CompoundShape.class, shape);
    }

    @Test
    void convexHullThrowsWhenNoMeshData() {
        assertThrows(IllegalStateException.class, () ->
            resolver.resolve(CollisionGeometry.convexHull(), null));
    }

    @Test
    void triangleMeshThrowsWhenNoMeshData() {
        assertThrows(IllegalStateException.class, () ->
            resolver.resolve(CollisionGeometry.triangleMesh(), null));
    }
}
