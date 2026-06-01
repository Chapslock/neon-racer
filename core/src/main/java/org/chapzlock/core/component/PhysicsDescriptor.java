package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import com.bulletphysics.collision.shapes.CollisionShape;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * Describes how a physics body should be created for an entity.
 *
 * <p>At scene load time, the engine derives a collision shape from the entity's
 * loaded mesh (convex hull for dynamic bodies, triangle mesh for static bodies)
 * unless an explicit shape is provided. The descriptor is consumed and replaced
 * with a {@link PhysicsBody} component.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * PhysicsDescriptor.dynamic(1f)                    // 1kg, auto convex hull
 * PhysicsDescriptor.dynamic(5f, myBoxShape)        // 5kg, explicit shape
 * PhysicsDescriptor.asStatic()                     // immovable, auto trimesh
 * }</pre>
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class PhysicsDescriptor implements Component {

    /** Mass in kilograms. Zero means static (immovable). */
    private final float mass;

    /**
     * Explicit collision shape. When null, the shape is derived from the
     * entity's mesh (convex hull for dynamic, BVH triangle mesh for static).
     */
    private final CollisionShape shape;

    /** Coefficient of restitution (bounciness). 0 = no bounce, 1 = perfect bounce. */
    @Builder.Default
    private final float restitution = 0f;

    /** Creates a dynamic body with automatic collision shape from mesh. */
    public static PhysicsDescriptor dynamic(float mass) {
        return builder().mass(mass).build();
    }

    /** Creates a dynamic body with an explicit collision shape. */
    public static PhysicsDescriptor dynamic(float mass, CollisionShape shape) {
        return builder().mass(mass).shape(shape).build();
    }

    /** Creates a static (immovable) body with automatic collision shape from mesh. */
    public static PhysicsDescriptor asStatic() {
        return builder().mass(0f).build();
    }

    /** Creates a static body with an explicit collision shape. */
    public static PhysicsDescriptor asStatic(CollisionShape shape) {
        return builder().mass(0f).shape(shape).build();
    }

    /** Returns true if this body has non-zero mass (dynamic simulation). */
    public boolean isDynamic() {
        return mass > 0f;
    }
}
