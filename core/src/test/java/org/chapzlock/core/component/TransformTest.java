package org.chapzlock.core.component;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformTest {

    @Test
    void defaultTransformHasIdentityMatrices() {
        Transform t = new Transform();
        Matrix4f local = t.getLocalMatrix();
        Matrix4f world = t.getWorldMatrix();

        Matrix4f identity = new Matrix4f();
        assertMatrixEquals(identity, local, 0.001f);
        assertMatrixEquals(identity, world, 0.001f);
    }

    @Test
    void setPositionMarksLocalDirty() {
        Transform t = new Transform();
        t.getLocalMatrix(); // clear dirty

        t.setPosition(5, 10, 15);

        Vector3f dest = new Vector3f();
        t.getPosition(dest);
        assertEquals(5f, dest.x, 0.001f);
        assertEquals(10f, dest.y, 0.001f);
        assertEquals(15f, dest.z, 0.001f);

        Matrix4f expected = new Matrix4f().translation(5, 10, 15);
        assertMatrixEquals(expected, t.getLocalMatrix(), 0.001f);
    }

    @Test
    void getPositionReturnsCopyNotReference() {
        Transform t = Transform.at(1, 2, 3);
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        t.getPosition(a);
        t.getPosition(b);
        a.set(99, 99, 99);
        assertEquals(1f, b.x, 0.001f);
    }

    @Test
    void setScaleNonUniform() {
        Transform t = new Transform();
        t.setScale(2, 3, 4);

        Vector3f dest = new Vector3f();
        t.getScale(dest);
        assertEquals(2f, dest.x, 0.001f);
        assertEquals(3f, dest.y, 0.001f);
        assertEquals(4f, dest.z, 0.001f);
    }

    @Test
    void setScaleUniform() {
        Transform t = new Transform();
        t.setScale(5);

        Vector3f dest = new Vector3f();
        t.getScale(dest);
        assertEquals(5f, dest.x, 0.001f);
        assertEquals(5f, dest.y, 0.001f);
        assertEquals(5f, dest.z, 0.001f);
    }

    @Test
    void localMatrixIncludesNonUniformScale() {
        Transform t = new Transform();
        t.setScale(2, 1, 3);
        Matrix4f m = t.getLocalMatrix();

        Vector3f col0 = new Vector3f();
        Vector3f col1 = new Vector3f();
        Vector3f col2 = new Vector3f();
        m.getColumn(0, col0);
        m.getColumn(1, col1);
        m.getColumn(2, col2);
        assertEquals(2f, col0.length(), 0.001f);
        assertEquals(1f, col1.length(), 0.001f);
        assertEquals(3f, col2.length(), 0.001f);
    }

    @Test
    void setRotationEulerSyncsToQuaternion() {
        Transform t = new Transform();
        t.setRotationEuler(0, 90, 0);

        Quaternionf dest = new Quaternionf();
        t.getRotationQuaternion(dest);

        Quaternionf expected = new Quaternionf().rotateY((float) Math.toRadians(90));
        assertEquals(expected.x, dest.x, 0.001f);
        assertEquals(expected.y, dest.y, 0.001f);
        assertEquals(expected.z, dest.z, 0.001f);
        assertEquals(expected.w, dest.w, 0.001f);
    }

    @Test
    void setRotationQuaternionSyncsToEuler() {
        Transform t = new Transform();
        Quaternionf q = new Quaternionf().rotateY((float) Math.toRadians(90));
        t.setRotationQuaternion(q);

        Vector3f euler = new Vector3f();
        t.getRotationEuler(euler);
        assertEquals(0f, euler.x, 0.5f);
        assertEquals(90f, euler.y, 0.5f);
        assertEquals(0f, euler.z, 0.5f);
    }

    @Test
    void getRotationEulerReturnsCopyNotReference() {
        Transform t = new Transform();
        t.setRotationEuler(10, 20, 30);
        Vector3f a = new Vector3f();
        t.getRotationEuler(a);
        a.set(99, 99, 99);

        Vector3f b = new Vector3f();
        t.getRotationEuler(b);
        assertEquals(10f, b.x, 0.001f);
    }

    @Test
    void getRotationQuaternionReturnsCopyNotReference() {
        Transform t = new Transform();
        Quaternionf q = new Quaternionf().rotateX(1f);
        t.setRotationQuaternion(q);

        Quaternionf a = new Quaternionf();
        t.getRotationQuaternion(a);
        a.identity();

        Quaternionf b = new Quaternionf();
        t.getRotationQuaternion(b);
        assertEquals(q.x, b.x, 0.001f);
    }

    @Test
    void worldMatrixReturnsLocalMatrixWhenNotInHierarchy() {
        Transform t = Transform.at(3, 6, 9);
        assertSame(t.getLocalMatrix(), t.getWorldMatrix());
    }

    @Test
    void worldMatrixReturnsSeparateMatrixWhenInHierarchy() {
        Transform t = new Transform();
        t.setInHierarchy(true);
        Matrix4f world = t.getWorldMatrix();
        Matrix4f local = t.getLocalMatrix();
        assertNotSame(local, world);
    }

    @Test
    void atFactoryWithPosition() {
        Transform t = Transform.at(1, 2, 3);
        Vector3f pos = new Vector3f();
        t.getPosition(pos);
        assertEquals(1f, pos.x);
        assertEquals(2f, pos.y);
        assertEquals(3f, pos.z);
    }

    @Test
    void atFactoryWithPositionAndRotation() {
        Transform t = Transform.at(1, 2, 3, 45, 90, 0);
        Vector3f pos = new Vector3f();
        Vector3f rot = new Vector3f();
        t.getPosition(pos);
        t.getRotationEuler(rot);
        assertEquals(1f, pos.x);
        assertEquals(45f, rot.x, 0.001f);
        assertEquals(90f, rot.y, 0.001f);
    }

    @Test
    void atFactoryWithUniformScale() {
        Transform t = Transform.at(0, 0, 0, 0, 0, 0, 2.5f);
        Vector3f scale = new Vector3f();
        t.getScale(scale);
        assertEquals(2.5f, scale.x, 0.001f);
        assertEquals(2.5f, scale.y, 0.001f);
        assertEquals(2.5f, scale.z, 0.001f);
    }

    @Test
    void atFactoryWithNonUniformScale() {
        Transform t = Transform.at(0, 0, 0, 0, 0, 0, 1, 2, 3);
        Vector3f scale = new Vector3f();
        t.getScale(scale);
        assertEquals(1f, scale.x, 0.001f);
        assertEquals(2f, scale.y, 0.001f);
        assertEquals(3f, scale.z, 0.001f);
    }

    @Test
    void convenienceGetters() {
        Transform t = Transform.at(7, 8, 9);
        assertEquals(7f, t.getX());
        assertEquals(8f, t.getY());
        assertEquals(9f, t.getZ());
    }

    private void assertMatrixEquals(Matrix4f expected, Matrix4f actual, float epsilon) {
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(col, row), actual.get(col, row), epsilon,
                    "Mismatch at col=" + col + ", row=" + row);
            }
        }
    }
}
