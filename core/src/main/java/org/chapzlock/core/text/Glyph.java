package org.chapzlock.core.text;

public record Glyph(
    float advance,
    float xBearing,
    float yBearing,
    float width,
    float height,
    float u0,
    float v0,
    float u1,
    float v1
) {}
