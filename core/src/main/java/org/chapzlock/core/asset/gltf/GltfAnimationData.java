package org.chapzlock.core.asset.gltf;

import java.util.List;

/**
 * Engine representation of a glTF animation clip.
 *
 * <p><strong>What is an animation clip?</strong> A single named animation (e.g., "Walk", "Idle",
 * "Attack") that drives one or more node properties over time. Each clip contains multiple
 * channels, where each channel animates one property (translation, rotation, scale, or morph
 * weights) of one target node.
 *
 * <p><strong>Why node indices, not entity IDs?</strong> At load time, entities have not been
 * created yet. The {@code targetNodeIndex} in each channel references a node by its position
 * in the glTF file's node list. During scene instantiation, the instantiator remaps these
 * indices to entity IDs when creating the engine's Animation and Channel objects.
 *
 * <p><strong>Duration:</strong> The total duration is the maximum keyframe time across all
 * channels. This is pre-computed at load time so the animation system can handle looping
 * and blending without scanning all keyframe arrays every frame.
 *
 * <p><strong>Lifecycle:</strong>
 * <ol>
 *   <li>{@link GltfLoader} extracts this from the glTF file</li>
 *   <li>Scene instantiator reads channels, remaps node indices to entity IDs</li>
 *   <li>Engine Animation/Channel components are created from this data</li>
 *   <li>This data object is then discarded</li>
 * </ol>
 *
 * @see GltfAsset
 * @see ChannelData
 */
public record GltfAnimationData(
    /** Animation clip name from glTF (e.g., "Walk", "Idle"). May be null for unnamed animations. */
    String name,
    /** Total duration in seconds (the latest keyframe time across all channels). */
    float duration,
    /** All channels in this animation clip. Each channel targets one node's property. */
    List<ChannelData> channels
) {
    /**
     * A single animation channel targeting one property of one node.
     *
     * <p><strong>What is a channel?</strong> An animation channel is the connection between
     * a set of keyframes and a specific property of a specific node. For example, a "Walk"
     * animation might have channels for the hip's translation, both legs' rotations, etc.
     *
     * <p><strong>Keyframe layout:</strong> The {@code keyframeTimes} array contains sorted
     * timestamps in seconds. The {@code keyframeValues} array contains the corresponding
     * values, laid out differently depending on the path:
     * <ul>
     *   <li>{@code "translation"} — 3 floats per keyframe (x, y, z)</li>
     *   <li>{@code "rotation"} — 4 floats per keyframe (x, y, z, w quaternion)</li>
     *   <li>{@code "scale"} — 3 floats per keyframe (x, y, z)</li>
     *   <li>{@code "weights"} — N floats per keyframe (one per morph target)</li>
     * </ul>
     *
     * <p>For CUBICSPLINE interpolation, each keyframe has 3x the values: in-tangent, value,
     * out-tangent (all contiguous in the values array).
     *
     * @see GltfAnimationData
     */
    public record ChannelData(
        /**
         * Index into {@link GltfAsset#nodes()} identifying which node this channel animates.
         *
         * <p>Will be remapped to an entity ID during scene instantiation.
         */
        int targetNodeIndex,
        /**
         * Which property is animated on the target node.
         *
         * <p>One of: {@code "translation"}, {@code "rotation"}, {@code "scale"}, or {@code "weights"}.
         */
        String path,
        /**
         * Interpolation method between keyframes.
         *
         * <p>One of:
         * <ul>
         *   <li>{@code "STEP"} — no interpolation, holds previous value until next keyframe</li>
         *   <li>{@code "LINEAR"} — linear interpolation (lerp for translation/scale, slerp for rotation)</li>
         *   <li>{@code "CUBICSPLINE"} — cubic spline with explicit tangents</li>
         * </ul>
         */
        String interpolation,
        /** Sorted keyframe timestamps in seconds. Length matches the number of keyframes. */
        float[] keyframeTimes,
        /**
         * Flat array of keyframe output values.
         *
         * <p>The layout depends on {@link #path()} and {@link #interpolation()}.
         * For LINEAR/STEP: {@code componentsPerKeyframe * numKeyframes} floats.
         * For CUBICSPLINE: {@code 3 * componentsPerKeyframe * numKeyframes} floats
         * (in-tangent + value + out-tangent per keyframe).
         */
        float[] keyframeValues
    ) {}
}
