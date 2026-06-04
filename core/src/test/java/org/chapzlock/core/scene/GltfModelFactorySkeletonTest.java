package org.chapzlock.core.scene;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.asset.gltf.*;
import org.chapzlock.core.component.Skeleton;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GltfModelFactorySkeletonTest {

    private Engine engine;
    private ComponentRegistry registry;
    private GltfModelFactory instantiator;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();
        instantiator = new GltfModelFactory(engine);
    }

    @Test
    void phase4CreatesSkeletonOnSkinnedNode() {
        GltfNodeData skinNode = new GltfNodeData("Mesh", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), -1, 0, List.of());
        GltfNodeData jointA = new GltfNodeData("JointA", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), -1, -1, List.of());
        GltfNodeData jointB = new GltfNodeData("JointB", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), -1, -1, List.of());

        Matrix4f ibmA = new Matrix4f().identity();
        Matrix4f ibmB = new Matrix4f().translate(1, 0, 0);

        GltfSkinData skin = new GltfSkinData("Armature", new int[]{1, 2}, new Matrix4f[]{ibmA, ibmB});

        GltfAsset asset = new GltfAsset(
            List.of(skinNode, jointA, jointB),
            List.of(0),
            List.of(),
            List.of(),
            List.of(skin),
            List.of()
        );

        GltfInstance instance = instantiator.instantiate(asset);

        int meshEntityId = instance.entity("Mesh");
        Skeleton skeleton = registry.getComponent(meshEntityId, Skeleton.class);

        assertNotNull(skeleton);
        assertEquals(2, skeleton.jointCount());
        int jointAEntity = instance.entity("JointA");
        int jointBEntity = instance.entity("JointB");
        assertEquals(jointAEntity, skeleton.getJointEntities()[0]);
        assertEquals(jointBEntity, skeleton.getJointEntities()[1]);
    }

    @Test
    void nodeWithoutSkinGetsNoSkeleton() {
        GltfNodeData node = new GltfNodeData("Box", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(node),
            List.of(0),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        GltfInstance instance = instantiator.instantiate(asset);
        int entityId = instance.entity("Box");
        assertNull(registry.getComponent(entityId, Skeleton.class));
    }
}
