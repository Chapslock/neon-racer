package org.chapzlock.core.text;

import java.nio.ByteBuffer;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * CPU-side representation of an MSDF font atlas.
 *
 * <p>Produced by MsdfAtlasGenerator and consumed by FontRegistry for GPU upload.
 * Contains all glyph metrics, atlas pixel data, and kerning pairs needed to render text.
 */
@Getter
@Builder
public class RawFontAtlas {
    /** Pixel data for the atlas (RGBA8 format). */
    private final ByteBuffer pixelData;

    /** Width of the atlas texture in pixels. */
    private final int atlasWidth;

    /** Height of the atlas texture in pixels. */
    private final int atlasHeight;

    /** Per-character glyph metrics and UV coordinates. */
    private final Map<Character, Glyph> glyphs;

    /** Per-character-pair kerning adjustments, keyed by {@link #kerningKey(char, char)}. */
    private final Map<Long, Float> kerning;

    /** Vertical distance from baseline to top of tallest glyph (pixels). */
    private final float lineHeight;

    /** Vertical distance from baseline to top of typical glyph (pixels). */
    private final float ascent;

    /** Vertical distance from baseline to bottom of lowest glyph (pixels, negative). */
    private final float descent;

    /** SDF distance range used during atlas generation (controls smoothness). */
    private final float distanceRange;

    /**
     * Compute a unique key for a kerning pair.
     *
     * @param left the left character
     * @param right the right character
     * @return a unique 64-bit key combining both characters
     */
    public static long kerningKey(char left, char right) {
        return ((long) left << 32) | (long) right;
    }
}
