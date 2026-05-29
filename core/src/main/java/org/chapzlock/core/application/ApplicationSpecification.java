package org.chapzlock.core.application;

import java.util.List;

import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.scene.Scene;
import org.chapzlock.core.window.WindowSpecification;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Properties for the application.
 */
@Builder
@Getter
@Setter
public class ApplicationSpecification {
    @Builder.Default
    private String name = "Application";
    private WindowSpecification windowSpec;
    @Builder.Default
    private List<GameSystem> engineSystems = List.of();
    private Scene initialScene;
}
