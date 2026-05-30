package org.chapzlock.core.physics;

import lombok.Builder;
import lombok.Getter;

/**
 * Global configuration for the physics system.
 *
 * <p>Currently only controls debug rendering.
 */
@Builder
@Getter
public class PhysicsSystemSpecs {
    /**
     * Configures whether the physics system should render additional information about physics simulation
     */
    @Builder.Default
    private boolean isDebugEnabled = false;


}
