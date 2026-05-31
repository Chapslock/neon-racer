package org.chapzlock.core.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MorphTargetsTest {

    @Test
    void constructorWithCount_initializesWeightsToZero() {
        MorphTargets morph = new MorphTargets(4);

        assertEquals(4, morph.getTargetCount());
        float[] weights = morph.getWeights();
        assertEquals(4, weights.length);
        for (float w : weights) {
            assertEquals(0.0f, w);
        }
    }

    @Test
    void constructorWithInitialWeights_copiesValues() {
        float[] initial = {0.5f, 0.8f, 0.2f};
        MorphTargets morph = new MorphTargets(initial);

        assertEquals(3, morph.getTargetCount());
        assertArrayEquals(new float[]{0.5f, 0.8f, 0.2f}, morph.getWeights());
    }

    @Test
    void constructorWithInitialWeights_doesNotShareArray() {
        float[] initial = {0.5f, 0.8f};
        MorphTargets morph = new MorphTargets(initial);

        initial[0] = 9.9f;
        assertEquals(0.5f, morph.getWeights()[0], "Modifying source array should not affect component");
    }

    @Test
    void setWeight_modifiesSingleWeight() {
        MorphTargets morph = new MorphTargets(3);

        morph.setWeight(1, 0.75f);

        assertEquals(0.0f, morph.getWeights()[0]);
        assertEquals(0.75f, morph.getWeights()[1]);
        assertEquals(0.0f, morph.getWeights()[2]);
    }

    @Test
    void setWeights_copiesFromSourceArray() {
        MorphTargets morph = new MorphTargets(3);
        float[] source = {0.1f, 0.2f, 0.3f};

        morph.setWeights(source);

        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, morph.getWeights());
    }

    @Test
    void setWeights_shorterSourceOnlySetsAvailableWeights() {
        MorphTargets morph = new MorphTargets(4);
        morph.setWeight(3, 0.9f);

        morph.setWeights(new float[]{0.1f, 0.2f});

        assertEquals(0.1f, morph.getWeights()[0]);
        assertEquals(0.2f, morph.getWeights()[1]);
        assertEquals(0.0f, morph.getWeights()[2]);
        assertEquals(0.9f, morph.getWeights()[3], "Weights beyond source length should be unchanged");
    }

    @Test
    void getTargetCount_returnsCorrectCount() {
        assertEquals(1, new MorphTargets(1).getTargetCount());
        assertEquals(8, new MorphTargets(8).getTargetCount());
    }

    @Test
    void positionDeltaVbos_nullByDefault() {
        MorphTargets morph = new MorphTargets(2);
        assertNull(morph.getPositionDeltaVbos());
    }

    @Test
    void positionDeltaVbos_setAndGet() {
        MorphTargets morph = new MorphTargets(2);
        int[] vbos = {101, 102};

        morph.setPositionDeltaVbos(vbos);

        assertArrayEquals(new int[]{101, 102}, morph.getPositionDeltaVbos());
    }

    @Test
    void normalDeltaVbos_nullByDefault() {
        MorphTargets morph = new MorphTargets(2);
        assertNull(morph.getNormalDeltaVbos());
    }

    @Test
    void normalDeltaVbos_setAndGet() {
        MorphTargets morph = new MorphTargets(3);
        int[] vbos = {201, 202, 203};

        morph.setNormalDeltaVbos(vbos);

        assertArrayEquals(new int[]{201, 202, 203}, morph.getNormalDeltaVbos());
    }
}
