package org.chapzlock.core.window;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for a GLFW window.
 *
 * <p>Fields: {@code title}, {@code width}, {@code height}, {@code isResizable}, {@code isVSyncEnabled}.
 */
@Builder
@Getter
@Setter
public class WindowSpecification {
    @Builder.Default
    private String title = "Application";
    @Builder.Default
    private int width = 600;
    @Builder.Default
    private int height = 400;
    @Builder.Default
    private boolean isResizable = true;
    @Builder.Default
    private boolean isVSyncEnabled = true;

    /** Returns the window aspect ratio (width / height). */
    public float getAspectRatio() {
        return (float) width / (float) height;
    }
}
