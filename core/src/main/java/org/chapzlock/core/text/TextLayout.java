package org.chapzlock.core.text;

import java.util.List;

/**
 * Output of text layout computation.
 *
 * <p>Produced by TextLayoutUtil and consumed by TextRenderer to build vertex buffers.
 * Contains a list of quads (one per character) and overall text bounds.
 *
 * @param quads list of positioned and textured character quads
 * @param bounds overall dimensions and line count of the laid-out text
 */
public record TextLayout(List<TextQuad> quads, TextBounds bounds) {

    /**
     * A single character in laid-out text.
     *
     * <p>Combines screen-space position and size with texture coordinates and color.
     *
     * @param x screen-space X position (pixels)
     * @param y screen-space Y position (pixels)
     * @param width quad width in screen space (pixels)
     * @param height quad height in screen space (pixels)
     * @param u0 atlas U coordinate for top-left corner (normalized, 0–1)
     * @param v0 atlas V coordinate for top-left corner (normalized, 0–1)
     * @param u1 atlas U coordinate for bottom-right corner (normalized, 0–1)
     * @param v1 atlas V coordinate for bottom-right corner (normalized, 0–1)
     * @param r red channel (0–1)
     * @param g green channel (0–1)
     * @param b blue channel (0–1)
     * @param a alpha channel (0–1)
     */
    public record TextQuad(
        float x, float y,
        float width, float height,
        float u0, float v0,
        float u1, float v1,
        float r, float g, float b, float a
    ) {}
}
