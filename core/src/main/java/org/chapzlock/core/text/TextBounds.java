package org.chapzlock.core.text;

import org.chapzlock.core.ecs.Component;

public record TextBounds(float width, float height, int lineCount) implements Component {}
