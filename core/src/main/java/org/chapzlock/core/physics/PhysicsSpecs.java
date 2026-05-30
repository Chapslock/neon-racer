package org.chapzlock.core.physics;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for a rigid body's physical properties.
 *
 * <p>Used when creating {@link org.chapzlock.core.component.PhysicsBody}.
 * A mass of 0 indicates a static object (infinite mass).
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
