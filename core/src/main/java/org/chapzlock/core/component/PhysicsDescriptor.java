package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.physics.CollisionGeometry;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Declares how a physics body should be created for an entity in the scene builder.
 *
 * <p>This is a "descriptor" component — it exists only during scene loading and is consumed
 * by the engine to produce a {@link PhysicsBody} component. You never see this at runtime.</p>
 *
 * <p>Usage in scene builder:</p>
 * <pre>{@code
 * .entity("player", e -> e
 *     .transform(Transform.at(0, 3, 0))
 *     .gltfModel("glb/player.glb")
 *     .physics(PhysicsDescriptor.dynamic(1f)
 *         .shape(CollisionGeometry.capsule(0.4f, 1.8f))
 *         .layer(GameLayers.PLAYER)
 *         .collidesWith(GameLayers.TERRAIN | GameLayers.ENEMY)))
 *
 * .entity("terrain", e -> e
 *     .transform(Transform.at(0, 0, 0))
 *     .gltfModel("glb/terrain.glb")
 *     .physics(PhysicsDescriptor.asStatic()
 *         .shape(CollisionGeometry.triangleMesh())))
 * }</pre>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PhysicsDescriptor implements Component {

    private final float mass;
    private final float friction;
    private final float restitution;
    private final float linearDamping;
    private final float angularDamping;
    private final CollisionGeometry geometry;
    private final int layer;
    private final int collisionMask;

    public static PhysicsDescriptor dynamic(float mass) {
        return new PhysicsDescriptor(mass, 0.5f, 0.1f, 0.05f, 0.85f, null, 1, 0xFFFFFFFF);
    }

    public static PhysicsDescriptor asStatic() {
        return new PhysicsDescriptor(0f, 0.5f, 0.1f, 0.05f, 0.85f, null, 1, 0xFFFFFFFF);
    }

    public PhysicsDescriptor shape(CollisionGeometry geometry) {
        return new PhysicsDescriptor(mass, friction, restitution, linearDamping, angularDamping, geometry, layer, collisionMask);
    }

    public PhysicsDescriptor friction(float friction) {
        return new PhysicsDescriptor(mass, friction, restitution, linearDamping, angularDamping, geometry, layer, collisionMask);
    }

    public PhysicsDescriptor restitution(float restitution) {
        return new PhysicsDescriptor(mass, friction, restitution, linearDamping, angularDamping, geometry, layer, collisionMask);
    }

    public PhysicsDescriptor linearDamping(float linearDamping) {
        return new PhysicsDescriptor(mass, friction, restitution, linearDamping, angularDamping, geometry, layer, collisionMask);
    }

    public PhysicsDescriptor angularDamping(float angularDamping) {
        return new PhysicsDescriptor(mass, friction, restitution, linearDamping, angularDamping, geometry, layer, collisionMask);
    }

    public PhysicsDescriptor layer(int layer) {
        return new PhysicsDescriptor(mass, friction, restitution, linearDamping, angularDamping, geometry, layer, collisionMask);
    }

    public PhysicsDescriptor collidesWith(int collisionMask) {
        return new PhysicsDescriptor(mass, friction, restitution, linearDamping, angularDamping, geometry, layer, collisionMask);
    }

    public boolean isDynamic() {
        return mass > 0f;
    }
}
