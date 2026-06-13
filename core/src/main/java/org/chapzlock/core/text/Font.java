package org.chapzlock.core.text;

import org.chapzlock.core.component.Texture;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Runtime font representation with GPU atlas texture and glyph metrics.
 *
 * <p>Created by FontRegistry from a RawFontAtlas. Stores a GPU texture handle
 * and all layout data needed to compute text positions.
 *
 * @see RawFontAtlas
 */
@Getter
@AllArgsConstructor
public class Font {
    /** GPU texture atlas containing all glyph images. */
    private final Texture atlasTexture;

    /** Per-character glyph metrics and UV coordinates. */
    private final Map<Character, Glyph> glyphs;

    /** Per-character-pair kerning adjustments (keyed by kerningKey). */
    private final Map<Long, Float> kerning;

    /** Vertical distance from baseline to top of tallest glyph (pixels). */
    private final float lineHeight;

    /** Vertical distance from baseline to top of typical glyph (pixels, used for scaling). */
    private final float ascent;

    /** Vertical distance from baseline to bottom of lowest glyph (pixels, negative). */
    private final float descent;

    /** SDF distance range used during atlas generation (controls smoothness). */
    private final float distanceRange;

    /**
     * Get the glyph for a character, falling back to '?' if not found.
     *
     * @param c the character to look up
     * @return the glyph metrics for the character, or '?' as fallback
     */
    public Glyph getGlyph(char c) {
        return glyphs.getOrDefault(c, glyphs.get('?'));
    }

    /**
     * Get the kerning adjustment between two characters.
     *
     * @param left the left character
     * @param right the right character
     * @return the kerning offset in pixels (0 if no kerning defined)
     */
    public float getKerning(char left, char right) {
        return kerning.getOrDefault(RawFontAtlas.kerningKey(left, right), 0f);
    }
}
