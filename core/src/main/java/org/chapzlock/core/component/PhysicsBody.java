package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.physics.CollisionGeometry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Marks an entity for physics simulation and describes its physical properties.
 *
 * <h3>How physics bodies work (mental model)</h3>
 * <p>When an entity has this component plus a {@link Transform}, the physics system creates
 * a corresponding "rigid body" in its internal simulation world. Each frame, forces (gravity,
 * collisions, your applied impulses) are computed and the entity's Transform is updated to
 * reflect where the body ended up.</p>
 *
 * <h3>Static vs Dynamic</h3>
 * <ul>
 *   <li><strong>Static (mass = 0)</strong> — never moves. Other objects bounce off it.
 *       Used for terrain, walls, platforms. Very cheap to simulate.</li>
 *   <li><strong>Dynamic (mass &gt; 0)</strong> — affected by gravity and forces.
 *       Used for players, projectiles, physics objects.</li>
 * </ul>
 *
 * <h3>Collision layers</h3>
 * <p>The {@code layer} field declares what this body IS (a bitmask). The {@code collisionMask}
 * field declares what this body COLLIDES WITH. Two bodies A and B collide only if
 * {@code (A.layer & B.collisionMask) != 0 AND (B.layer & A.collisionMask) != 0}.
 * Define layer constants in your game code (not in the engine).</p>
 *
 * <h3>This component is data-only</h3>
 * <p>It holds no physics engine internals. To apply forces or query velocity, use the
 * {@link org.chapzlock.core.physics.Physics} facade from {@code EngineContext.getPhysics()}.</p>
 *
 * @see CollisionGeometry
 * @see org.chapzlock.core.physics.Physics
 * @see org.chapzlock.core.physics.PhysicsSystem
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PhysicsBody implements Component {
    private final float mass;
    @Builder.Default
    private final float friction = 0.5f;
    @Builder.Default
    private final float restitution = 0.1f;
    @Builder.Default
    private final float linearDamping = 0.05f;
    @Builder.Default
    private final float angularDamping = 0.85f;
    private final CollisionGeometry geometry;
    @Builder.Default
    private final float angularFactor = 1f;
    @Builder.Default
    private final int layer = 1;
    @Builder.Default
    private final int collisionMask = 0xFFFFFFFF;

    public boolean isStatic() {
        return mass == 0f;
    }

    public boolean isDynamic() {
        return mass > 0f;
    }

    // Static factory methods

    public static PhysicsBody dynamic(float mass) {
        return new PhysicsBody(mass, 0.5f, 0.1f, 0.05f, 0.85f, null, 1f, 1, 0xFFFFFFFF);
    }

    public static PhysicsBody asStatic() {
        return new PhysicsBody(0f, 0.5f, 0.1f, 0.05f, 0.85f, null, 1f, 1, 0xFFFFFFFF);
    }

    // Fluent wither methods

    public PhysicsBody shape(CollisionGeometry geometry) {
        return new PhysicsBody(mass, friction, restitution, linearDamping, angularDamping, geometry, angularFactor, layer, collisionMask);
    }

    public PhysicsBody friction(float friction) {
        return new PhysicsBody(mass, friction, restitution, linearDamping, angularDamping, geometry, angularFactor, layer, collisionMask);
    }

    public PhysicsBody restitution(float restitution) {
        return new PhysicsBody(mass, friction, restitution, linearDamping, angularDamping, geometry, angularFactor, layer, collisionMask);
    }

    public PhysicsBody linearDamping(float linearDamping) {
        return new PhysicsBody(mass, friction, restitution, linearDamping, angularDamping, geometry, angularFactor, layer, collisionMask);
    }

    public PhysicsBody angularDamping(float angularDamping) {
        return new PhysicsBody(mass, friction, restitution, linearDamping, angularDamping, geometry, angularFactor, layer, collisionMask);
    }

    public PhysicsBody angularFactor(float angularFactor) {
        return new PhysicsBody(mass, friction, restitution, linearDamping, angularDamping, geometry, angularFactor, layer, collisionMask);
    }

    public PhysicsBody lockRotation() {
        return angularFactor(0f);
    }

    public PhysicsBody layer(int layer) {
        return new PhysicsBody(mass, friction, restitution, linearDamping, angularDamping, geometry, angularFactor, layer, collisionMask);
    }

    public PhysicsBody collidesWith(int collisionMask) {
        return new PhysicsBody(mass, friction, restitution, linearDamping, angularDamping, geometry, angularFactor, layer, collisionMask);
    }
}
