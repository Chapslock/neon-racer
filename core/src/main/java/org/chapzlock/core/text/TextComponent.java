package org.chapzlock.core.text;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector2f;
import org.joml.Vector4f;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TextComponent implements Component {
    private final String text;
    private final String font;
    private final float fontSize;
    private final Vector4f color;
    @Builder.Default
    private final TextAlignment alignment = TextAlignment.LEFT;
    @Builder.Default
    private final float maxWidth = 0f;
    private final Vector4f outlineColor;
    @Builder.Default
    private final float outlineWidth = 0f;
    private final Vector4f shadowColor;
    private final Vector2f shadowOffset;
}
