package org.chapzlock.core.application;

import org.chapzlock.core.physics.PhysicsWorldConfig;
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
 *   <li>{@code windowSpec} — window configuration</li>
 *   <li>{@code physicsConfig} — physics world configuration (gravity, time step, debug rendering)</li>
 *   <li>{@code initialScene} — first scene to load on startup</li>
 * </ul>
 */
@Builder
@Getter
@Setter
public class ApplicationSpecification {
    /** Window configuration. */
    private WindowSpecification windowSpec;
    /** Physics world configuration. Defaults to standard Earth gravity, 60Hz, no debug. */
    @Builder.Default
    private PhysicsWorldConfig physicsConfig = PhysicsWorldConfig.defaults();
    /** First scene to load on startup. */
    private Scene initialScene;
}
