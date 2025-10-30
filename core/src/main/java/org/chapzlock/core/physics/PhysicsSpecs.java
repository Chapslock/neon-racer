package org.chapzlock.core.physics;

import lombok.Builder;
import lombok.Getter;

/**
 * Used to configure physics properties
 */
@Builder
@Getter
public class PhysicsSpecs {
    /**
     * Mass of the physics object. A mass of 0 indicates a static physics object
     */
    @Builder.Default
    private float mass = 0f;
    /**
     * Standard sliding friction
     */
    @Builder.Default
    private float friction = 0.5f;
    /**
     * The bounciness of the object
     */
    @Builder.Default
    private float restitution = 0.1f;

    /**
     * Air resistance
     */
    @Builder.Default
    private float linearDamping = 0.05f;

    /**
     * Rotational damping. Helps reduce sliding and stabilize motion
     */
    @Builder.Default
    private float angularDamping = 0.85f;
}
