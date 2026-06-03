package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.RawMeshDataFactory;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.PhysicsDescriptor;
import org.chapzlock.core.component.SpriteMaterial;
import org.chapzlock.core.component.SpriteDescriptor;
import org.chapzlock.core.component.SpriteLayer;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.Tag;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.TextureSystem;

/**
 * Resolves descriptor components into GPU-backed runtime components during
 * scene instantiation.
 *
 * <p>Each descriptor type has a corresponding resolution method that loads assets,
 * selects shaders, creates materials, and registers them with the engine.
 * Descriptors are consumed — they are not retained on the entity.</p>
 */
public class DescriptorResolver {

    private final EngineContext ctx;

    public DescriptorResolver(EngineContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Resolves all descriptors on a blueprint into runtime components.
     *
     * @return ordered list of components to attach to the entity
     */
    public List<Component> resolve(EntityBlueprint blueprint) {
        List<Component> result = new ArrayList<>();

        Mesh mesh = null;

        if (blueprint.getSpriteDescriptor() != null) {
            mesh = resolveSprite(blueprint.getSpriteDescriptor(), result);
        }

        if (blueprint.getPhysicsDescriptor() != null) {
            resolvePhysics(blueprint.getPhysicsDescriptor(), result);
        }

        for (Tag tag : blueprint.getTags()) {
            result.add(tag);
        }

        result.addAll(blueprint.getComponents());

        return result;
    }

    private Mesh resolveSprite(SpriteDescriptor desc, List<Component> result) {
        MeshSystem meshSystem = ctx.getMeshSystem();
        TextureSystem textureSystem = ctx.getTextureSystem();

        Texture texture = textureSystem.load(desc.getTexture());
        Mesh mesh = meshSystem.load(RawMeshDataFactory.createUi());
        result.add(mesh);
        result.add(new SpriteMaterial(texture, desc.getTint()));
        result.add(new SpriteLayer());

        return mesh;
    }

    private void resolvePhysics(PhysicsDescriptor desc, List<Component> result) {
        if (desc.getGeometry() == null) {
            throw new IllegalStateException(
                "PhysicsDescriptor has no CollisionGeometry. " +
                "Call .shape(CollisionGeometry.capsule(...)) or similar on the descriptor. " +
                "Available shapes: box, sphere, capsule, convexHull, triangleMesh, compound.");
        }

        result.add(PhysicsBody.builder()
            .mass(desc.getMass())
            .friction(desc.getFriction())
            .restitution(desc.getRestitution())
            .linearDamping(desc.getLinearDamping())
            .angularDamping(desc.getAngularDamping())
            .angularFactor(desc.getAngularFactor())
            .geometry(desc.getGeometry())
            .layer(desc.getLayer())
            .collisionMask(desc.getCollisionMask())
            .build());
    }
}
