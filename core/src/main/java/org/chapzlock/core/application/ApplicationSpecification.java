package org.chapzlock.core.application;

import java.util.List;

import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.scene.Scene;
import org.chapzlock.core.window.WindowSpecification;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for the application.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code name} — application name (unused for now)</li>
 *   <li>{@code windowSpec} — window configuration</li>
 *   <li>{@code engineSystems} — always-active systems (e.g., PhysicsSystem, RenderSystem)</li>
 *   <li>{@code initialScene} — first scene to load on startup</li>
 * </ul>
 */
@Builder
@Getter
@Setter
public class ApplicationSpecification {
    /** Application name. */
    @Builder.Default
    private String name = "Application";
    /** Window configuration. */
    private WindowSpecification windowSpec;
    /** Always-active systems (e.g., PhysicsSystem, RenderSystem). */
    @Builder.Default
    private List<GameSystem> engineSystems = List.of();
    /** First scene to load on startup. */
    private Scene initialScene;
}
