package org.chapzlock.core.text;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextComponentTest {

    @Test
    void builder_createsComponentWithDefaults() {
        TextComponent tc = TextComponent.builder()
                .text("Hello")
                .font("fonts/Inconsolata.ttf")
                .fontSize(24f)
                .color(new Vector4f(1, 1, 1, 1))
                .build();

        assertEquals("Hello", tc.getText());
        assertEquals("fonts/Inconsolata.ttf", tc.getFont());
        assertEquals(24f, tc.getFontSize());
        assertEquals(new Vector4f(1, 1, 1, 1), tc.getColor());
        assertEquals(TextAlignment.LEFT, tc.getAlignment());
        assertEquals(0f, tc.getMaxWidth());
        assertNull(tc.getOutlineColor());
        assertEquals(0f, tc.getOutlineWidth());
        assertNull(tc.getShadowColor());
        assertNull(tc.getShadowOffset());
    }

    @Test
    void toBuilder_createsImmutableCopy() {
        TextComponent original = TextComponent.builder()
                .text("Original")
                .font("fonts/Inconsolata.ttf")
                .fontSize(32f)
                .color(new Vector4f(1, 1, 1, 1))
                .build();

        TextComponent updated = original.toBuilder().text("Updated").build();

        assertEquals("Original", original.getText());
        assertEquals("Updated", updated.getText());
        assertEquals(32f, updated.getFontSize());
    }

    @Test
    void builder_withEffects() {
        TextComponent tc = TextComponent.builder()
                .text("Neon")
                .font("fonts/Inconsolata.ttf")
                .fontSize(48f)
                .color(new Vector4f(0, 1, 1, 1))
                .outlineColor(new Vector4f(1, 1, 1, 1))
                .outlineWidth(0.15f)
                .shadowColor(new Vector4f(0, 0, 0, 0.5f))
                .shadowOffset(new Vector2f(2, 2))
                .build();

        assertEquals(new Vector4f(1, 1, 1, 1), tc.getOutlineColor());
        assertEquals(0.15f, tc.getOutlineWidth());
        assertEquals(new Vector4f(0, 0, 0, 0.5f), tc.getShadowColor());
        assertEquals(new Vector2f(2, 2), tc.getShadowOffset());
    }

    @Test
    void textBounds_holdsComputedDimensions() {
        TextBounds bounds = new TextBounds(150f, 48f, 2);

        assertEquals(150f, bounds.width());
        assertEquals(48f, bounds.height());
        assertEquals(2, bounds.lineCount());
    }
}
