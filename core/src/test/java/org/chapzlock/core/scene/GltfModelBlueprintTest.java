package org.chapzlock.core.scene;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.gltf.GltfAnimationData;
import org.chapzlock.core.asset.gltf.GltfAsset;
import org.chapzlock.core.asset.gltf.GltfNodeData;
import org.chapzlock.core.component.AnimationClipSet;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link GltfSceneInstantiator#instantiateAsChild(GltfAsset, int)}.
 *
 * <p>Verifies that glTF models instantiated as children of an existing entity are
 * correctly parented in the hierarchy, and that animation components are attached
 * to the parent entity rather than the glTF root node.
 */
class GltfModelBlueprintTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private GltfSceneInstantiator instantiator;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        instantiator = new GltfSceneInstantiator(ctx);
    }

    @Test
    void instantiateAsChildParentsUnderGivenEntity() {
        // Create a parent entity that the glTF model will be parented under
        int parent = Entity.create(registry);
        Transform parentTransform = Transform.at(10, 20, 30, 0, 0, 0);
        Hierarchy parentHierarchy = Hierarchy.root();
        registry.addComponents(parent, parentTransform, parentHierarchy);

        // Create a minimal glTF asset with one named node
        GltfNodeData child = new GltfNodeData(
            "Child",
            new Vector3f(1, 2, 3),
            new Quaternionf(),
            new Vector3f(1, 1, 1),
            -1, -1,
            List.of()
        );
        GltfAsset asset = new GltfAsset(
            List.of(child),
            List.of(0),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        GltfInstance instance = instantiator.instantiateAsChild(asset, parent);

        // The child's hierarchy should point to the parent entity
        int childEntity = instance.entity("Child");
        Hierarchy childHierarchy = registry.getComponent(childEntity, Hierarchy.class);
        assertNotNull(childHierarchy, "Child entity should have a Hierarchy component");
        assertEquals(parent, childHierarchy.getParent(),
            "Child's hierarchy parent should be the provided parent entity");

        // The parent should list the child in its children array
        int[] parentChildren = parentHierarchy.getChildren();
        boolean found = IntStream.of(parentChildren).anyMatch(id -> id == childEntity);
        assertTrue(found, "Parent hierarchy should contain the child entity");
    }

    @Test
    void instantiateAsChildAttachesAnimationsToParent() {
        // Create a parent entity (the "owner" of the model)
        int parent = Entity.create(registry);
        registry.addComponents(parent, new Transform(), Hierarchy.root());

        // Create a glTF asset with one node and one animation clip
        GltfNodeData node = new GltfNodeData(
            "Node",
            new Vector3f(),
            new Quaternionf(),
            new Vector3f(1, 1, 1),
            -1, -1,
            List.of()
        );
        GltfAnimationData.ChannelData ch = new GltfAnimationData.ChannelData(
            0, "translation", "LINEAR",
            new float[]{0f, 1f},
            new float[]{0, 0, 0, 1, 2, 3}
        );
        GltfAnimationData anim = new GltfAnimationData("Idle", 1f, List.of(ch));

        GltfAsset asset = new GltfAsset(
            List.of(node),
            List.of(0),
            List.of(),
            List.of(),
            List.of(),
            List.of(anim)
        );

        instantiator.instantiateAsChild(asset, parent);

        // AnimationClipSet should be on the parent entity (not on the glTF root node)
        AnimationClipSet clips = registry.getComponent(parent, AnimationClipSet.class);
        assertNotNull(clips, "AnimationClipSet should be attached to the parent entity");
        assertNotNull(clips.get("Idle"), "The 'Idle' clip should be present in the clip set");
    }

    @Test
    void instantiateAsChildCreatesParentHierarchyIfMissing() {
        // Create a parent entity WITHOUT a Hierarchy component
        int parent = Entity.create(registry);
        registry.addComponent(parent, new Transform());

        // Create a minimal glTF asset with one node
        GltfNodeData node = new GltfNodeData(
            "Node",
            new Vector3f(),
            new Quaternionf(),
            new Vector3f(1, 1, 1),
            -1, -1,
            List.of()
        );
        GltfAsset asset = new GltfAsset(
            List.of(node),
            List.of(0),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        instantiator.instantiateAsChild(asset, parent);

        // Should have created a Hierarchy component for the parent automatically
        Hierarchy parentHierarchy = registry.getComponent(parent, Hierarchy.class);
        assertNotNull(parentHierarchy, "Parent should have a Hierarchy component created for it");
        assertEquals(1, parentHierarchy.getChildCount(),
            "Parent hierarchy should have exactly one child (the glTF root node)");
    }

    @Test
    void instantiateAsChildHandlesMultipleRootNodes() {
        // Create a parent entity
        int parent = Entity.create(registry);
        registry.addComponents(parent, new Transform(), Hierarchy.root());

        // Create a glTF asset with two root nodes (some glTF files have multiple roots)
        GltfNodeData nodeA = new GltfNodeData(
            "RootA",
            new Vector3f(1, 0, 0),
            new Quaternionf(),
            new Vector3f(1, 1, 1),
            -1, -1,
            List.of()
        );
        GltfNodeData nodeB = new GltfNodeData(
            "RootB",
            new Vector3f(0, 1, 0),
            new Quaternionf(),
            new Vector3f(1, 1, 1),
            -1, -1,
            List.of()
        );
        GltfAsset asset = new GltfAsset(
            List.of(nodeA, nodeB),
            List.of(0, 1),   // Both are root nodes
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        GltfInstance instance = instantiator.instantiateAsChild(asset, parent);

        // Both root nodes should be children of the parent
        Hierarchy parentHierarchy = registry.getComponent(parent, Hierarchy.class);
        assertEquals(2, parentHierarchy.getChildCount(),
            "Parent hierarchy should have both glTF root nodes as children");

        // Both should have Hierarchy pointing to parent
        int entityA = instance.entity("RootA");
        int entityB = instance.entity("RootB");
        assertEquals(parent, registry.getComponent(entityA, Hierarchy.class).getParent());
        assertEquals(parent, registry.getComponent(entityB, Hierarchy.class).getParent());
    }

    @Test
    void instantiateAsChildPreservesNodeTransforms() {
        int parent = Entity.create(registry);
        registry.addComponents(parent, new Transform(), Hierarchy.root());

        Vector3f expectedPos = new Vector3f(5, 10, 15);
        GltfNodeData node = new GltfNodeData(
            "Positioned",
            expectedPos,
            new Quaternionf(),
            new Vector3f(2, 2, 2),
            -1, -1,
            List.of()
        );
        GltfAsset asset = new GltfAsset(
            List.of(node),
            List.of(0),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        GltfInstance instance = instantiator.instantiateAsChild(asset, parent);

        int childEntity = instance.entity("Positioned");
        Transform childTransform = registry.getComponent(childEntity, Transform.class);
        assertNotNull(childTransform);
        assertEquals(expectedPos.x, childTransform.getPosition().x, 0.001f);
        assertEquals(expectedPos.y, childTransform.getPosition().y, 0.001f);
        assertEquals(expectedPos.z, childTransform.getPosition().z, 0.001f);

    }
}
