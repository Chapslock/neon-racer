package org.chapzlock.core.physics;

import lombok.Builder;
import lombok.Getter;

/**
 * Used for providing a custom configuration for the Physics system
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
