package org.chapzlock.core.asset.gltf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Unpacks raw binary data from glTF accessors into typed Java arrays.
 *
 * <p><strong>What is a glTF accessor?</strong> glTF stores vertex data (positions, normals, etc.)
 * as raw bytes in binary buffers. An "accessor" describes how to interpret those bytes:
 * what type (float, int, short), how many components (scalar, vec2, vec3, vec4),
 * how many elements, and where in the buffer they start.
 *
 * <p><strong>Why do we need this?</strong> The GPU and the engine's mesh system expect typed arrays
 * (float[] for positions/normals/UVs, int[] for index buffers). But glTF stores everything as raw
 * bytes in a single binary blob. This class bridges that gap by reading bytes at the correct
 * offsets and interpreting them as the correct numeric type.
 *
 * <p><strong>Key glTF concepts:</strong>
 * <ul>
 *   <li><em>componentCount</em> — how many numbers per element. A vec3 position has 3 components,
 *       a vec2 UV has 2, a scalar index has 1.</li>
 *   <li><em>elementCount</em> — how many elements (vertices, indices, etc.) to read.</li>
 *   <li><em>byteOffset</em> — where in the buffer to start reading. Buffers can contain multiple
 *       accessors at different offsets.</li>
 *   <li><em>byteStride</em> — the number of bytes between the start of consecutive elements.
 *       When 0, data is tightly packed (no gaps). A non-zero stride allows interleaved vertex
 *       attributes (e.g., position + normal + UV stored together per vertex).</li>
 * </ul>
 *
 * <p>All methods use absolute indexing into the buffer (via {@link ByteBuffer#getFloat(int)},
 * etc.) so they do not modify the buffer's position.
 *
 * @see GltfAsset
 */
public final class AccessorUnpacker {

    private AccessorUnpacker() {
    }

    /**
     * Unpacks float data from a byte buffer.
     *
     * <p>This is the most common unpacking operation in glTF loading. Vertex positions (vec3),
     * normals (vec3), texture coordinates (vec2), and tangents (vec4) are all stored as floats.
     *
     * <p><strong>Example:</strong> To unpack 100 vec3 positions starting at byte 0 with no
     * interleaving: {@code unpackFloats(buffer, 3, 100, 0, 0)} returns a float[300].
     *
     * @param buffer         the raw byte buffer containing the binary data
     * @param componentCount number of floats per element (e.g., 3 for vec3, 2 for vec2)
     * @param elementCount   number of elements (vertices, etc.) to read
     * @param byteOffset     starting byte offset into the buffer where this accessor's data begins
     * @param byteStride     bytes between the start of consecutive elements (0 = tightly packed,
     *                       meaning each element immediately follows the previous one)
     * @return flat float array of size {@code componentCount * elementCount}
     */
    public static float[] unpackFloats(ByteBuffer buffer, int componentCount, int elementCount,
                                       int byteOffset, int byteStride) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int stride = byteStride > 0 ? byteStride : componentCount * 4;
        float[] result = new float[componentCount * elementCount];

        for (int i = 0; i < elementCount; i++) {
            int elementOffset = byteOffset + i * stride;
            for (int c = 0; c < componentCount; c++) {
                result[i * componentCount + c] = buffer.getFloat(elementOffset + c * 4);
            }
        }
        return result;
    }

    /**
     * Unpacks 32-bit signed integer data from a byte buffer.
     *
     * <p>Used in glTF for index buffers when the UNSIGNED_INT component type is specified
     * (for meshes with more than 65535 vertices), and for joint indices in some skeletal
     * animation setups.
     *
     * <p><strong>Example:</strong> To unpack 300 scalar indices (triangles):
     * {@code unpackInts(buffer, 1, 300, 0, 0)} returns an int[300].
     *
     * @param buffer         the raw byte buffer containing the binary data
     * @param componentCount number of ints per element (e.g., 1 for scalar indices, 4 for ivec4 joints)
     * @param elementCount   number of elements to read
     * @param byteOffset     starting byte offset into the buffer
     * @param byteStride     bytes between consecutive elements (0 = tightly packed)
     * @return flat int array of size {@code componentCount * elementCount}
     */
    public static int[] unpackInts(ByteBuffer buffer, int componentCount, int elementCount,
                                   int byteOffset, int byteStride) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int stride = byteStride > 0 ? byteStride : componentCount * 4;
        int[] result = new int[componentCount * elementCount];

        for (int i = 0; i < elementCount; i++) {
            int elementOffset = byteOffset + i * stride;
            for (int c = 0; c < componentCount; c++) {
                result[i * componentCount + c] = buffer.getInt(elementOffset + c * 4);
            }
        }
        return result;
    }

    /**
     * Unpacks unsigned 16-bit shorts from a byte buffer as int[].
     *
     * <p>This is the most common format for glTF index buffers. The UNSIGNED_SHORT component
     * type (5123) uses 2 bytes per value and supports meshes with up to 65535 vertices.
     * Since Java's {@code short} is signed (-32768 to 32767), we convert to {@code int}
     * to preserve the full unsigned range (0 to 65535).
     *
     * <p><strong>Example:</strong> To unpack 300 triangle indices:
     * {@code unpackUnsignedShorts(buffer, 1, 300, 0, 0)} returns an int[300] with values 0-65535.
     *
     * @param buffer         the raw byte buffer containing the binary data
     * @param componentCount number of shorts per element (usually 1 for indices)
     * @param elementCount   number of elements to read
     * @param byteOffset     starting byte offset into the buffer
     * @param byteStride     bytes between consecutive elements (0 = tightly packed)
     * @return flat int array with unsigned short values (0-65535)
     */
    public static int[] unpackUnsignedShorts(ByteBuffer buffer, int componentCount, int elementCount,
                                             int byteOffset, int byteStride) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int stride = byteStride > 0 ? byteStride : componentCount * 2;
        int[] result = new int[componentCount * elementCount];

        for (int i = 0; i < elementCount; i++) {
            int elementOffset = byteOffset + i * stride;
            for (int c = 0; c < componentCount; c++) {
                result[i * componentCount + c] = Short.toUnsignedInt(
                        buffer.getShort(elementOffset + c * 2));
            }
        }
        return result;
    }

    /**
     * Unpacks unsigned 8-bit bytes from a byte buffer as int[].
     *
     * <p>Used in glTF for joint indices when the UNSIGNED_BYTE component type (5121) is specified.
     * This is a compact format for skeletal animation that supports up to 256 joints per mesh.
     * Since Java's {@code byte} is signed (-128 to 127), we convert to {@code int} to preserve
     * the full unsigned range (0 to 255).
     *
     * <p><strong>Example:</strong> To unpack 100 vertices' joint indices (4 joints per vertex):
     * {@code unpackUnsignedBytes(buffer, 4, 100, 0, 0)} returns an int[400] with values 0-255.
     *
     * @param buffer         the raw byte buffer containing the binary data
     * @param componentCount number of bytes per element (e.g., 4 for joint indices)
     * @param elementCount   number of elements to read
     * @param byteOffset     starting byte offset into the buffer
     * @param byteStride     bytes between consecutive elements (0 = tightly packed)
     * @return flat int array with unsigned byte values (0-255)
     */
    public static int[] unpackUnsignedBytes(ByteBuffer buffer, int componentCount, int elementCount,
                                            int byteOffset, int byteStride) {
        int stride = byteStride > 0 ? byteStride : componentCount;
        int[] result = new int[componentCount * elementCount];

        for (int i = 0; i < elementCount; i++) {
            int elementOffset = byteOffset + i * stride;
            for (int c = 0; c < componentCount; c++) {
                result[i * componentCount + c] = Byte.toUnsignedInt(
                        buffer.get(elementOffset + c));
            }
        }
        return result;
    }
}
