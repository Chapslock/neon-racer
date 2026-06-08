package org.chapzlock.core.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlyphTest {

    @Test
    void glyph_holdsMetricsAndUvCoordinates() {
        Glyph glyph = new Glyph(8.0f, 1.0f, 10.0f, 7.0f, 12.0f, 0.1f, 0.2f, 0.3f, 0.4f);

        assertEquals(8.0f, glyph.advance());
        assertEquals(1.0f, glyph.xBearing());
        assertEquals(10.0f, glyph.yBearing());
        assertEquals(7.0f, glyph.width());
        assertEquals(12.0f, glyph.height());
        assertEquals(0.1f, glyph.u0());
        assertEquals(0.2f, glyph.v0());
        assertEquals(0.3f, glyph.u1());
        assertEquals(0.4f, glyph.v1());
    }
}
