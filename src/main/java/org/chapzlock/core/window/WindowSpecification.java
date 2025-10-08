package org.chapzlock.core.window;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class WindowSpecification {
    private String title;
    @Builder.Default
    private int width = 600;
    @Builder.Default
    private int height = 400;
    @Builder.Default
    private boolean isResizable = true;
    @Builder.Default
    private boolean isVSyncEnabled = true;

    public float getAspectRatio() {
        return (float) width / (float) height;
    }
}
