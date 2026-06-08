package org.chapzlock.core.text;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextLayoutUtilTest {

    private Font font;

    @BeforeEach
    void setUp() {
        // Simple test font: each glyph is 8x12, advance 10, bearing (1, 12)
        // Scale will be 1.0 when fontSize == ascent (both 12)
        Map<Character, Glyph> glyphs = Map.of(
            'A', new Glyph(10f, 1f, 12f, 8f, 12f, 0.0f, 0.0f, 0.1f, 0.1f),
            'B', new Glyph(10f, 1f, 12f, 8f, 12f, 0.1f, 0.0f, 0.2f, 0.1f),
            ' ', new Glyph(5f, 0f, 0f, 0f, 0f, 0.0f, 0.0f, 0.0f, 0.0f)
        );
        font = new Font(null, glyphs, Map.of(), 16f, 12f, 4f, 4f);
    }

    @Test
    void singleLine_leftAligned_positionsGlyphsSequentially() {
        TextLayout layout = TextLayoutUtil.layout(
                "AB", font, 12f, 0f, TextAlignment.LEFT,
                new Vector4f(1, 1, 1, 1), null, null);

        // Scale = 12/12 = 1.0
        // Glyph A: x = 0 + xBearing(1) = 1, y = 0 + (ascent(12) - yBearing(12)) = 0
        // Glyph B: x = advance(10) + xBearing(1) = 11, y = 0
        assertEquals(2, layout.quads().size());

        TextLayout.TextQuad firstQuad = layout.quads().get(0);
        assertEquals(1f, firstQuad.x(), 0.001f);
        assertEquals(0f, firstQuad.y(), 0.001f);
        assertEquals(8f, firstQuad.width(), 0.001f);
        assertEquals(12f, firstQuad.height(), 0.001f);

        TextLayout.TextQuad secondQuad = layout.quads().get(1);
        assertEquals(11f, secondQuad.x(), 0.001f);
        assertEquals(0f, secondQuad.y(), 0.001f);

        // Bounds: width = total advance = 10 + 10 = 20, height = lineHeight = 16
        assertEquals(20f, layout.bounds().width(), 0.001f);
        assertEquals(16f, layout.bounds().height(), 0.001f);
        assertEquals(1, layout.bounds().lineCount());
    }

    @Test
    void spaceCharacter_advancesCursorButEmitsNoQuad() {
        TextLayout layout = TextLayoutUtil.layout(
                "A B", font, 12f, 0f, TextAlignment.LEFT,
                new Vector4f(1, 1, 1, 1), null, null);

        // Only 2 visible quads (space has 0 width/height)
        assertEquals(2, layout.quads().size());

        // Second visible glyph 'B' at: advance(A) + advance(space) + xBearing(B) = 10 + 5 + 1 = 16
        TextLayout.TextQuad bQuad = layout.quads().get(1);
        assertEquals(16f, bQuad.x(), 0.001f);
    }

    @Test
    void newline_wrapsToNextLine() {
        TextLayout layout = TextLayoutUtil.layout(
                "A\nB", font, 12f, 0f, TextAlignment.LEFT,
                new Vector4f(1, 1, 1, 1), null, null);

        assertEquals(2, layout.quads().size());

        // First line: A at y=0
        assertEquals(0f, layout.quads().get(0).y(), 0.001f);
        // Second line: B at y=lineHeight(16)
        assertEquals(16f, layout.quads().get(1).y(), 0.001f);
        // B's x resets to xBearing = 1
        assertEquals(1f, layout.quads().get(1).x(), 0.001f);

        assertEquals(2, layout.bounds().lineCount());
        assertEquals(32f, layout.bounds().height(), 0.001f); // 2 lines * 16
    }

    @Test
    void wordWrap_breaksAtLastWhitespace() {
        // "AB AB" with maxWidth=25
        // "AB" = 2*10 = 20px advance, space = 5px
        // "AB AB" = 10+10+5+10+10 = 45px, exceeds maxWidth 25
        // Should wrap at the space: "AB" on line 1, "AB" on line 2
        TextLayout layout = TextLayoutUtil.layout(
                "AB AB", font, 12f, 25f, TextAlignment.LEFT,
                new Vector4f(1, 1, 1, 1), null, null);

        assertEquals(2, layout.bounds().lineCount());
        assertEquals(20f, layout.bounds().width(), 0.001f);
    }

    @Test
    void wordWrap_fallsBackToCharacterWrapForLongWord() {
        // "AAAA" = 4*10 = 40px, maxWidth = 25px, no whitespace
        // Should character-wrap: "AA" (20px) fits on line 1, "AA" on line 2
        TextLayout layout = TextLayoutUtil.layout(
                "AAAA", font, 12f, 25f, TextAlignment.LEFT,
                new Vector4f(1, 1, 1, 1), null, null);

        assertEquals(2, layout.bounds().lineCount());
    }

    @Test
    void wordWrap_noWrapWhenMaxWidthIsZero() {
        TextLayout layout = TextLayoutUtil.layout(
                "AB AB", font, 12f, 0f, TextAlignment.LEFT,
                new Vector4f(1, 1, 1, 1), null, null);

        assertEquals(1, layout.bounds().lineCount());
    }

    @Test
    void centerAlignment_shiftsQuadsToCenter() {
        TextLayout layout = TextLayoutUtil.layout(
                "A", font, 12f, 100f, TextAlignment.CENTER,
                new Vector4f(1, 1, 1, 1), null, null);

        // Single 'A': advance = 10px, container = 100px
        // Line width = 10 (advance of A)
        // Offset = (100 - 10) / 2 = 45
        // Quad x = offset + xBearing = 45 + 1 = 46
        TextLayout.TextQuad quad = layout.quads().get(0);
        assertEquals(46f, quad.x(), 0.5f);
    }

    @Test
    void rightAlignment_shiftsQuadsToRight() {
        TextLayout layout = TextLayoutUtil.layout(
                "A", font, 12f, 100f, TextAlignment.RIGHT,
                new Vector4f(1, 1, 1, 1), null, null);

        // Offset = 100 - 10 = 90
        // Quad x = offset + xBearing = 90 + 1 = 91
        TextLayout.TextQuad quad = layout.quads().get(0);
        assertEquals(91f, quad.x(), 0.5f);
    }

    @Test
    void shadow_prependsShadowQuadsBeforeMainQuads() {
        TextLayout layout = TextLayoutUtil.layout(
                "A", font, 12f, 0f, TextAlignment.LEFT,
                new Vector4f(1, 1, 1, 1),
                new Vector4f(0, 0, 0, 0.5f), new Vector2f(3, 3));

        // Should have 2 quads: shadow first, then main
        assertEquals(2, layout.quads().size());

        TextLayout.TextQuad shadow = layout.quads().get(0);
        TextLayout.TextQuad main = layout.quads().get(1);

        // Shadow is offset by (3, 3) from main
        assertEquals(main.x() + 3f, shadow.x(), 0.001f);
        assertEquals(main.y() + 3f, shadow.y(), 0.001f);

        // Shadow has shadow color
        assertEquals(0f, shadow.r(), 0.001f);
        assertEquals(0f, shadow.g(), 0.001f);
        assertEquals(0f, shadow.b(), 0.001f);
        assertEquals(0.5f, shadow.a(), 0.001f);

        // Main has original color
        assertEquals(1f, main.r(), 0.001f);
        assertEquals(1f, main.a(), 0.001f);
    }
}
