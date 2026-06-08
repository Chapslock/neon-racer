package org.chapzlock.core.text;

import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import lombok.experimental.UtilityClass;

/**
 * Stateless text layout computation.
 *
 * <p>Converts a string into a list of positioned character quads using font metrics.
 * Supports alignment, shadows, and multi-line text (word wrapping will be added in a future task).
 */
@UtilityClass
public class TextLayoutUtil {

    /**
     * Compute glyph positions for a string of text.
     *
     * <p><strong>Algorithm:</strong>
     * <ol>
     *   <li>Compute scale = fontSize / font.ascent</li>
     *   <li>For each character: apply kerning, emit quad if visible, advance cursor</li>
     *   <li>On newline or width overflow: advance to next line (word wrap at whitespace if possible)</li>
     *   <li>After all chars: apply alignment offsets per line</li>
     *   <li>If shadow: prepend shadow quads (copies shifted by shadowOffset)</li>
     * </ol>
     *
     * @param text the text to lay out (may contain '\n' for manual line breaks)
     * @param font the font containing glyph metrics and texture coordinates
     * @param fontSize the desired font size in pixels
     * @param maxWidth the maximum width for text wrapping (0 = no limit)
     * @param alignment horizontal alignment within the container (LEFT/CENTER/RIGHT)
     * @param color the text color (RGBA, 0–1 range)
     * @param shadowColor the shadow color (RGBA, 0–1 range, null = no shadow)
     * @param shadowOffset the shadow offset in pixels (null = no shadow)
     * @return the computed layout with positioned quads and bounds
     */
    public static TextLayout layout(
        String text,
        Font font,
        float fontSize,
        float maxWidth,
        TextAlignment alignment,
        Vector4f color,
        Vector4f shadowColor,
        Vector2f shadowOffset
    ) {
        // Compute scale factor: fontSize relative to font's ascent
        float scale = fontSize / font.getAscent();
        float scaledLineHeight = font.getLineHeight() * scale;

        // Build quads for all characters
        List<TextLayout.TextQuad> quads = new ArrayList<>();
        List<LineData> lines = new ArrayList<>();

        float cursorX = 0f;
        float cursorY = 0f;
        float lineStartX = 0f;
        char prevChar = '\0';

        // Track current line
        int lineStartQuadIndex = 0;
        float lineMaxWidth = 0f;

        // Track last whitespace for word wrapping
        int lastSpaceIndex = -1;          // index in text string of last space in current line
        int lastSpaceQuadIndex = -1;      // quad index at the start of the space
        float cursorXBeforeLastSpace = -1f; // cursor position before the space (line width without trailing space)

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Handle newline
            if (c == '\n') {
                // Record current line
                lines.add(new LineData(lineStartQuadIndex, quads.size(), cursorX));
                lineMaxWidth = Math.max(lineMaxWidth, cursorX);

                // Advance to next line
                cursorY += scaledLineHeight;
                cursorX = lineStartX;
                lineStartQuadIndex = quads.size();
                prevChar = '\0';

                // Reset word-wrap tracking
                lastSpaceIndex = -1;
                lastSpaceQuadIndex = -1;
                cursorXBeforeLastSpace = -1f;
                continue;
            }

            // Look up glyph
            Glyph glyph = font.getGlyph(c);
            if (glyph == null) {
                continue;
            }

            // Apply kerning from previous character
            if (prevChar != '\0') {
                float kerning = font.getKerning(prevChar, c);
                cursorX += kerning * scale;
            }

            // Check for width overflow before emitting quad
            float advanceWidth = glyph.advance() * scale;
            boolean wouldOverflow = maxWidth > 0 && (cursorX + advanceWidth) > maxWidth;

            if (wouldOverflow) {
                // Try word wrap at last space
                if (lastSpaceIndex >= 0) {
                    // Remove quads after the space (including space itself, which has no quad)
                    int quadsToRemove = quads.size() - lastSpaceQuadIndex;
                    for (int j = 0; j < quadsToRemove; j++) {
                        quads.remove(quads.size() - 1);
                    }

                    // Record current line (up to but not including the space)
                    lines.add(new LineData(lineStartQuadIndex, lastSpaceQuadIndex, cursorXBeforeLastSpace));
                    lineMaxWidth = Math.max(lineMaxWidth, cursorXBeforeLastSpace);

                    // Advance to next line
                    cursorY += scaledLineHeight;
                    cursorX = lineStartX;
                    lineStartQuadIndex = quads.size();

                    // Reset word-wrap tracking
                    int resumeIndex = lastSpaceIndex;
                    lastSpaceIndex = -1;
                    lastSpaceQuadIndex = -1;
                    cursorXBeforeLastSpace = -1f;

                    // Rewind text processing to character after the space
                    i = resumeIndex; // Will be incremented by loop, so we'll process resumeIndex+1 next
                    prevChar = '\0';
                    continue;
                } else {
                    // No space found, character wrap: start new line right here
                    lines.add(new LineData(lineStartQuadIndex, quads.size(), cursorX));
                    lineMaxWidth = Math.max(lineMaxWidth, cursorX);

                    cursorY += scaledLineHeight;
                    cursorX = lineStartX;
                    lineStartQuadIndex = quads.size();

                    // Reset word-wrap tracking
                    lastSpaceIndex = -1;
                    lastSpaceQuadIndex = -1;
                    cursorXBeforeLastSpace = -1f;

                    // Process current character on new line
                    prevChar = '\0';
                    // Don't continue — fall through to process current character
                }
            }

            // Track whitespace for word wrapping (before processing the space itself)
            if (Character.isWhitespace(c)) {
                lastSpaceIndex = i;
                lastSpaceQuadIndex = quads.size(); // quads emitted up to this point (space won't emit a quad)
                cursorXBeforeLastSpace = cursorX; // cursor position before the space's advance
            }

            // Emit quad if glyph has visible area
            if (glyph.width() > 0 && glyph.height() > 0) {
                float quadX = cursorX + glyph.xBearing() * scale;
                float quadY = cursorY + (font.getAscent() - glyph.yBearing()) * scale;
                float quadWidth = glyph.width() * scale;
                float quadHeight = glyph.height() * scale;

                quads.add(new TextLayout.TextQuad(
                    quadX, quadY,
                    quadWidth, quadHeight,
                    glyph.u0(), glyph.v0(),
                    glyph.u1(), glyph.v1(),
                    color.x, color.y, color.z, color.w
                ));
            }

            // Advance cursor
            cursorX += advanceWidth;
            prevChar = c;
        }

        // Record last line
        if (lineStartQuadIndex < quads.size() || text.endsWith("\n")) {
            lines.add(new LineData(lineStartQuadIndex, quads.size(), cursorX));
            lineMaxWidth = Math.max(lineMaxWidth, cursorX);
        }

        // If text is empty, create a single empty line
        if (lines.isEmpty()) {
            lines.add(new LineData(0, 0, 0f));
        }

        // Apply alignment offsets
        float containerWidth = maxWidth > 0 ? maxWidth : lineMaxWidth;
        applyAlignment(quads, lines, alignment, containerWidth);

        // Compute overall bounds
        float totalWidth = lineMaxWidth;
        float totalHeight = lines.size() * scaledLineHeight;
        TextBounds bounds = new TextBounds(totalWidth, totalHeight, lines.size());

        // Prepend shadow quads if shadow is enabled
        if (shadowColor != null && shadowOffset != null) {
            List<TextLayout.TextQuad> shadowQuads = new ArrayList<>(quads.size());
            for (TextLayout.TextQuad quad : quads) {
                shadowQuads.add(new TextLayout.TextQuad(
                    quad.x() + shadowOffset.x,
                    quad.y() + shadowOffset.y,
                    quad.width(), quad.height(),
                    quad.u0(), quad.v0(),
                    quad.u1(), quad.v1(),
                    shadowColor.x, shadowColor.y, shadowColor.z, shadowColor.w
                ));
            }
            // Prepend shadow quads (render shadow first, then main text on top)
            shadowQuads.addAll(quads);
            quads = shadowQuads;
        }

        return new TextLayout(quads, bounds);
    }

    /**
     * Apply horizontal alignment offsets to all quads.
     *
     * @param quads the list of quads to modify
     * @param lines metadata about each line (start/end quad indices and width)
     * @param alignment the desired alignment
     * @param containerWidth the container width to align within
     */
    private static void applyAlignment(
        List<TextLayout.TextQuad> quads,
        List<LineData> lines,
        TextAlignment alignment,
        float containerWidth
    ) {
        if (alignment == TextAlignment.LEFT) {
            return; // No offset needed for left alignment
        }

        for (LineData line : lines) {
            float lineWidth = line.width;
            float offsetX = 0f;

            if (alignment == TextAlignment.CENTER) {
                offsetX = (containerWidth - lineWidth) / 2f;
            } else if (alignment == TextAlignment.RIGHT) {
                offsetX = containerWidth - lineWidth;
            }

            // Apply offset to all quads in this line
            for (int i = line.startQuadIndex; i < line.endQuadIndex; i++) {
                TextLayout.TextQuad quad = quads.get(i);
                quads.set(i, new TextLayout.TextQuad(
                    quad.x() + offsetX,
                    quad.y(),
                    quad.width(), quad.height(),
                    quad.u0(), quad.v0(),
                    quad.u1(), quad.v1(),
                    quad.r(), quad.g(), quad.b(), quad.a()
                ));
            }
        }
    }

    /**
     * Metadata about a single line of text.
     */
    private record LineData(int startQuadIndex, int endQuadIndex, float width) {}
}
