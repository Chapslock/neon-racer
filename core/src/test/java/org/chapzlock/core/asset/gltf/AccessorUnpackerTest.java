package org.chapzlock.core.asset.gltf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessorUnpackerTest {

    @Test
    void unpackFloatsFromByteBuffer() {
        // Simulate 3 vec3 positions packed as little-endian floats
        float[] expected = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f};
        ByteBuffer buffer = ByteBuffer.allocate(expected.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : expected) {
            buffer.putFloat(f);
        }
        buffer.flip();

        float[] result = AccessorUnpacker.unpackFloats(buffer, 3, 3, 0, 0);
        assertArrayEquals(expected, result, 0.001f);
    }

    @Test
    void unpackFloatsWithByteOffset() {
        // 4 bytes padding, then 2 vec2 UVs
        float[] expected = {0.5f, 0.5f, 1.0f, 0.0f};
        ByteBuffer buffer = ByteBuffer.allocate(4 + expected.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0); // 4 bytes padding
        for (float f : expected) {
            buffer.putFloat(f);
        }
        buffer.flip();

        float[] result = AccessorUnpacker.unpackFloats(buffer, 2, 2, 4, 0);
        assertArrayEquals(expected, result, 0.001f);
    }

    @Test
    void unpackIntsFromByteBuffer() {
        int[] expected = {0, 1, 2, 3, 4, 5, 6, 7};
        ByteBuffer buffer = ByteBuffer.allocate(expected.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int i : expected) {
            buffer.putInt(i);
        }
        buffer.flip();

        int[] result = AccessorUnpacker.unpackInts(buffer, 4, 2, 0, 0);
        assertArrayEquals(expected, result);
    }

    @Test
    void unpackUnsignedShortsAsInts() {
        // glTF often uses unsigned short (2 bytes) for indices
        int[] expected = {0, 1, 2, 100, 200, 300};
        ByteBuffer buffer = ByteBuffer.allocate(expected.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i : expected) {
            buffer.putShort((short) i);
        }
        buffer.flip();

        int[] result = AccessorUnpacker.unpackUnsignedShorts(buffer, 1, 6, 0, 0);
        assertArrayEquals(expected, result);
    }
}
