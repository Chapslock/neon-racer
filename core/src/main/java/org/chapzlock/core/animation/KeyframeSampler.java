package org.chapzlock.core.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Samples animation channel values at a given time by interpolating between keyframes.
 *
 * <p><strong>What is keyframe sampling?</strong> Animations store values only at specific
 * time points (keyframes). To get the value at an arbitrary time between keyframes,
 * we interpolate. This class finds the surrounding keyframes and blends between them.
 *
 * <p><strong>Why is this needed?</strong> If an animation defines a character's hand at
 * position (0, 0, 0) at time=0.0 and (10, 5, 0) at time=1.0, we need to compute
 * where the hand should be at time=0.3 (answer: roughly (3, 1.5, 0) with linear interpolation).
 *
 * <p><strong>Algorithm:</strong>
 * <ol>
 *   <li>Binary search the keyframe times array to find the two keyframes bracketing the query time</li>
 *   <li>Compute the interpolation factor: {@code t = (queryTime - prevTime) / (nextTime - prevTime)}</li>
 *   <li>Blend the prev/next values using the channel's interpolation mode</li>
 * </ol>
 *
 * <p>For {@link Interpolation#LINEAR} interpolation:
 * <ul>
 *   <li>TRANSLATION and SCALE: standard linear interpolation (lerp) on each component</li>
 *   <li>ROTATION: spherical linear interpolation (slerp) to avoid distortion and gimbal lock</li>
 * </ul>
 *
 * <p>For {@link Interpolation#STEP} interpolation:
 * <ul>
 *   <li>The value snaps instantly to the previous keyframe's value with no blending.
 *       Useful for on/off switches, visibility toggles, or intentionally jerky motion.</li>
 * </ul>
 *
 * <p><strong>Performance notes:</strong> This class uses no allocations for vec3 sampling.
 * Quaternion sampling allocates two temporary Quaternionf objects per call (needed for slerp).
 * All methods write results into caller-provided output objects to minimize GC pressure
 * during animation playback (which runs every frame).
 *
 * @see Channel
 * @see Interpolation
 * @see TargetPath
 */
public final class KeyframeSampler {

    private KeyframeSampler() {}

    /**
     * Samples a vec3 value (translation or scale) from a channel at the given time.
     *
     * <p>This handles the common case where an animation channel drives a 3-component
     * property like position or scale. The method finds which two keyframes surround
     * the query time, then interpolates between them based on the channel's
     * {@link Interpolation} mode.
     *
     * <p><strong>Example:</strong> Given keyframes at t=0 with value (0,0,0) and
     * t=1 with value (10,20,30), sampling at t=0.5 with LINEAR interpolation
     * returns (5, 10, 15).
     *
     * @param channel the animation channel to sample (must use TRANSLATION or SCALE path)
     * @param time    the current animation time in seconds
     * @param result  the vector to store the result in (reused to avoid allocations per frame)
     * @return the result vector (same reference passed in, for method chaining convenience)
     */
    public static Vector3f sampleVec3(Channel channel, float time, Vector3f result) {
        float[] times = channel.keyframeTimes();
        float[] values = channel.keyframeValues();
        int keyframeCount = channel.keyframeCount();

        // Edge case: time is before or at the first keyframe — clamp to first value
        if (time <= times[0]) {
            return result.set(values[0], values[1], values[2]);
        }
        // Edge case: time is at or after the last keyframe — clamp to last value
        if (time >= times[keyframeCount - 1]) {
            int last = (keyframeCount - 1) * 3;
            return result.set(values[last], values[last + 1], values[last + 2]);
        }

        // Binary search for the first keyframe whose time is strictly greater than query time.
        // This gives us the "next" keyframe; the "previous" is one index before.
        int nextIndex = findNextKeyframe(times, time);
        int prevIndex = nextIndex - 1;

        // STEP interpolation: return the previous keyframe's value without blending
        if (channel.interpolation() == Interpolation.STEP) {
            int offset = prevIndex * 3;
            return result.set(values[offset], values[offset + 1], values[offset + 2]);
        }

        // LINEAR interpolation: compute how far between the two keyframes we are (0.0 to 1.0)
        float prevTime = times[prevIndex];
        float nextTime = times[nextIndex];
        float t = (time - prevTime) / (nextTime - prevTime);

        int prevOffset = prevIndex * 3;
        int nextOffset = nextIndex * 3;

        // Lerp each component independently
        result.set(
            lerp(values[prevOffset], values[nextOffset], t),
            lerp(values[prevOffset + 1], values[nextOffset + 1], t),
            lerp(values[prevOffset + 2], values[nextOffset + 2], t)
        );
        return result;
    }

    /**
     * Samples a quaternion value (rotation) from a channel at the given time.
     *
     * <p>Rotations are stored as quaternions (4 components: x, y, z, w) and interpolated
     * using <strong>slerp</strong> (spherical linear interpolation). Slerp produces smooth
     * rotation along the shortest arc on a sphere, avoiding the distortion that would occur
     * if we simply lerped the quaternion components independently.
     *
     * <p><strong>Why slerp instead of lerp?</strong> Quaternions live on the surface of a
     * 4D unit sphere. Linear interpolation would cut through the interior of the sphere,
     * producing non-unit quaternions (which represent invalid rotations). Slerp follows the
     * great arc on the sphere surface, maintaining unit length and constant angular velocity.
     *
     * @param channel the animation channel to sample (must be ROTATION path)
     * @param time    the current animation time in seconds
     * @param result  the quaternion to store the result in (reused to avoid allocations)
     * @return the result quaternion (same reference passed in)
     */
    public static Quaternionf sampleQuaternion(Channel channel, float time, Quaternionf result) {
        float[] times = channel.keyframeTimes();
        float[] values = channel.keyframeValues();
        int keyframeCount = channel.keyframeCount();

        // Edge case: clamp to first keyframe
        if (time <= times[0]) {
            return result.set(values[0], values[1], values[2], values[3]);
        }
        // Edge case: clamp to last keyframe
        if (time >= times[keyframeCount - 1]) {
            int last = (keyframeCount - 1) * 4;
            return result.set(values[last], values[last + 1], values[last + 2], values[last + 3]);
        }

        int nextIndex = findNextKeyframe(times, time);
        int prevIndex = nextIndex - 1;

        // STEP: snap to previous keyframe rotation
        if (channel.interpolation() == Interpolation.STEP) {
            int offset = prevIndex * 4;
            return result.set(values[offset], values[offset + 1], values[offset + 2], values[offset + 3]);
        }

        // LINEAR: slerp between the two surrounding quaternion keyframes
        float prevTime = times[prevIndex];
        float nextTime = times[nextIndex];
        float t = (time - prevTime) / (nextTime - prevTime);

        int prevOffset = prevIndex * 4;
        int nextOffset = nextIndex * 4;

        // Create temporary quaternions for the slerp operation.
        // JOML's slerp writes the result into a destination quaternion.
        Quaternionf prev = new Quaternionf(
            values[prevOffset], values[prevOffset + 1],
            values[prevOffset + 2], values[prevOffset + 3]);
        Quaternionf next = new Quaternionf(
            values[nextOffset], values[nextOffset + 1],
            values[nextOffset + 2], values[nextOffset + 3]);

        prev.slerp(next, t, result);
        return result;
    }

    /**
     * Samples morph target weights from a channel at the given time.
     *
     * <p><strong>What are morph targets?</strong> Morph targets (also called blend shapes)
     * allow a mesh to smoothly deform between predefined shapes. For example, a face mesh
     * might have morph targets for "smile", "frown", and "blink". Each morph target has a
     * weight (0.0 = not applied, 1.0 = fully applied) that controls how much it influences
     * the final mesh shape.
     *
     * <p>This method interpolates an array of weights, where each element corresponds to
     * one morph target. The number of weights per keyframe is determined by the channel's
     * {@link Channel#componentsPerKeyframe()}.
     *
     * @param channel the animation channel (must use WEIGHTS path)
     * @param time    the current animation time in seconds
     * @param result  float array to store results (length must equal componentsPerKeyframe)
     * @return the result array (same reference passed in)
     */
    public static float[] sampleWeights(Channel channel, float time, float[] result) {
        float[] times = channel.keyframeTimes();
        float[] values = channel.keyframeValues();
        int keyframeCount = channel.keyframeCount();
        int componentCount = channel.componentsPerKeyframe();

        // Edge case: clamp to first keyframe weights
        if (time <= times[0]) {
            System.arraycopy(values, 0, result, 0, componentCount);
            return result;
        }
        // Edge case: clamp to last keyframe weights
        if (time >= times[keyframeCount - 1]) {
            int last = (keyframeCount - 1) * componentCount;
            System.arraycopy(values, last, result, 0, componentCount);
            return result;
        }

        int nextIndex = findNextKeyframe(times, time);
        int prevIndex = nextIndex - 1;

        // STEP: snap to previous keyframe's weights
        if (channel.interpolation() == Interpolation.STEP) {
            System.arraycopy(values, prevIndex * componentCount, result, 0, componentCount);
            return result;
        }

        // LINEAR: lerp each weight component independently
        float prevTime = times[prevIndex];
        float nextTime = times[nextIndex];
        float t = (time - prevTime) / (nextTime - prevTime);

        int prevOffset = prevIndex * componentCount;
        int nextOffset = nextIndex * componentCount;
        for (int i = 0; i < componentCount; i++) {
            result[i] = lerp(values[prevOffset + i], values[nextOffset + i], t);
        }
        return result;
    }

    /**
     * Binary search to find the index of the first keyframe time strictly greater than the query time.
     *
     * <p>This is equivalent to C++'s {@code std::upper_bound}. Given a sorted array of keyframe
     * times and a query time that falls between two keyframes, this returns the index of the
     * "next" keyframe (the one just after the query time). The "previous" keyframe is at
     * index (result - 1).
     *
     * <p><strong>Example:</strong> Given times [0.0, 0.5, 1.0, 1.5, 2.0] and query time 0.7:
     * <ul>
     *   <li>Returns 2 (the index of time 1.0, which is the first time &gt; 0.7)</li>
     *   <li>Previous keyframe is at index 1 (time 0.5)</li>
     * </ul>
     *
     * <p><strong>Complexity:</strong> O(log n) where n is the number of keyframes.
     *
     * @param times sorted array of keyframe times
     * @param time  the query time to search for
     * @return index of the first keyframe time strictly greater than the query time
     */
    static int findNextKeyframe(float[] times, float time) {
        int low = 0;
        int high = times.length - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (times[mid] <= time) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    /**
     * Standard linear interpolation between two float values.
     *
     * <p>Formula: {@code result = a + (b - a) * t} where t is in [0, 1].
     * When t=0 the result is a, when t=1 the result is b, and values in between
     * produce proportional blends.
     *
     * @param a the start value (returned when t=0)
     * @param b the end value (returned when t=1)
     * @param t the interpolation factor, typically in range [0, 1]
     * @return the interpolated value
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
