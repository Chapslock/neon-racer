package org.chapzlock.core.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyframeSamplerTest {

    @Test
    void sampleVec3LinearMidpoint() {
        // Two keyframes: t=0 -> (0,0,0), t=1 -> (10,20,30)
        Channel channel = new Channel(0, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f},
            new float[]{0f, 0f, 0f, 10f, 20f, 30f});

        Vector3f result = new Vector3f();
        KeyframeSampler.sampleVec3(channel, 0.5f, result);

        assertEquals(5f, result.x, 0.001f);
        assertEquals(10f, result.y, 0.001f);
        assertEquals(15f, result.z, 0.001f);
    }

    @Test
    void sampleVec3LinearAtQuarter() {
        Channel channel = new Channel(0, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 2f},
            new float[]{0f, 0f, 0f, 8f, 4f, 2f});

        Vector3f result = new Vector3f();
        KeyframeSampler.sampleVec3(channel, 0.5f, result);

        // t=0.5 out of 2.0 range -> factor 0.25
        assertEquals(2f, result.x, 0.001f);
        assertEquals(1f, result.y, 0.001f);
        assertEquals(0.5f, result.z, 0.001f);
    }

    @Test
    void sampleVec3BeforeFirstKeyframe() {
        Channel channel = new Channel(0, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{1f, 2f},
            new float[]{5f, 10f, 15f, 20f, 25f, 30f});

        Vector3f result = new Vector3f();
        KeyframeSampler.sampleVec3(channel, 0f, result);

        assertEquals(5f, result.x, 0.001f);
        assertEquals(10f, result.y, 0.001f);
        assertEquals(15f, result.z, 0.001f);
    }

    @Test
    void sampleVec3AfterLastKeyframe() {
        Channel channel = new Channel(0, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f},
            new float[]{0f, 0f, 0f, 10f, 20f, 30f});

        Vector3f result = new Vector3f();
        KeyframeSampler.sampleVec3(channel, 5f, result);

        assertEquals(10f, result.x, 0.001f);
        assertEquals(20f, result.y, 0.001f);
        assertEquals(30f, result.z, 0.001f);
    }

    @Test
    void sampleVec3StepReturnsCurrentKeyframe() {
        Channel channel = new Channel(0, TargetPath.TRANSLATION, Interpolation.STEP,
            new float[]{0f, 1f, 2f},
            new float[]{0f, 0f, 0f, 10f, 10f, 10f, 20f, 20f, 20f});

        Vector3f result = new Vector3f();
        KeyframeSampler.sampleVec3(channel, 0.5f, result);

        // STEP: should use keyframe at t=0, not interpolate toward t=1
        assertEquals(0f, result.x, 0.001f);
        assertEquals(0f, result.y, 0.001f);
        assertEquals(0f, result.z, 0.001f);
    }

    @Test
    void sampleQuaternionLinearMidpoint() {
        // Identity (0,0,0,1) to 90-degree Y rotation
        Quaternionf start = new Quaternionf().identity();
        Quaternionf end = new Quaternionf().rotationY((float) Math.toRadians(90));

        Channel channel = new Channel(0, TargetPath.ROTATION, Interpolation.LINEAR,
            new float[]{0f, 1f},
            new float[]{start.x, start.y, start.z, start.w, end.x, end.y, end.z, end.w});

        Quaternionf result = new Quaternionf();
        KeyframeSampler.sampleQuaternion(channel, 0.5f, result);

        // At midpoint, should be roughly 45-degree Y rotation
        Quaternionf expected = new Quaternionf().rotationY((float) Math.toRadians(45));
        assertEquals(expected.x, result.x, 0.01f);
        assertEquals(expected.y, result.y, 0.01f);
        assertEquals(expected.z, result.z, 0.01f);
        assertEquals(expected.w, result.w, 0.01f);
    }

    @Test
    void sampleQuaternionStep() {
        Quaternionf start = new Quaternionf().identity();
        Quaternionf end = new Quaternionf().rotationY((float) Math.toRadians(90));

        Channel channel = new Channel(0, TargetPath.ROTATION, Interpolation.STEP,
            new float[]{0f, 1f},
            new float[]{start.x, start.y, start.z, start.w, end.x, end.y, end.z, end.w});

        Quaternionf result = new Quaternionf();
        KeyframeSampler.sampleQuaternion(channel, 0.5f, result);

        // STEP: should be the identity (prev keyframe)
        assertEquals(start.x, result.x, 0.001f);
        assertEquals(start.y, result.y, 0.001f);
        assertEquals(start.z, result.z, 0.001f);
        assertEquals(start.w, result.w, 0.001f);
    }

    @Test
    void findNextKeyframeBinarySearch() {
        float[] times = {0f, 0.5f, 1.0f, 1.5f, 2.0f};

        assertEquals(1, KeyframeSampler.findNextKeyframe(times, 0.3f));
        assertEquals(2, KeyframeSampler.findNextKeyframe(times, 0.7f));
        assertEquals(4, KeyframeSampler.findNextKeyframe(times, 1.8f));
    }

    @Test
    void sampleVec3ThreeKeyframes() {
        // t=0->(0,0,0), t=1->(10,0,0), t=2->(10,10,0)
        Channel channel = new Channel(0, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f, 2f},
            new float[]{0f, 0f, 0f, 10f, 0f, 0f, 10f, 10f, 0f});

        Vector3f result = new Vector3f();

        // Between first and second keyframe
        KeyframeSampler.sampleVec3(channel, 0.5f, result);
        assertEquals(5f, result.x, 0.001f);
        assertEquals(0f, result.y, 0.001f);

        // Between second and third keyframe
        KeyframeSampler.sampleVec3(channel, 1.5f, result);
        assertEquals(10f, result.x, 0.001f);
        assertEquals(5f, result.y, 0.001f);
    }
}
