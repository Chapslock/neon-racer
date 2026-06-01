package org.chapzlock.core.scene;

import java.util.List;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.gltf.GltfAsset;
import org.chapzlock.core.asset.gltf.GltfNodeData;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.component.WorldTransform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GltfSceneInstantiator}.
 *
 * <p>These tests verify the non-GPU code paths: entity creation, hierarchy setup,
 * transform propagation, and name mapping. GPU-dependent paths (mesh upload, texture
 * loading, shader compilation) are skipped because {@code EngineContext.testContext()}
 * provides null GPU systems.
 *
 * <p>All test nodes use {@code meshIndex = -1} so the instantiator never attempts
 * GPU operations.
 */
class GltfSceneInstantiatorTest {

    private EngineContext ctx;
    private ComponentRegistry registry;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
    }

    @Test
    void instantiateCreatesEntitiesForNodes() {
        // A simple scene: root node with one child
        GltfNodeData root = new GltfNodeData(
            "Root", new Vector3f(1, 2, 3), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of(1));
        GltfNodeData child = new GltfNodeData(
            "Child", new Vector3f(4, 5, 6), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(root, child), List.of(0), List.of(), List.of(), List.of(), List.of());

        // Instantiate without GPU systems (no meshes to upload)
        GltfSceneInstantiator instantiator = new GltfSceneInstantiator(ctx);
        GltfInstance instance = instantiator.instantiate(asset);

        // Verify name mapping
        assertTrue(instance.hasEntity("Root"));
        assertTrue(instance.hasEntity("Child"));

        // Verify transforms
        int rootId = instance.entity("Root");
        Transform rootTransform = registry.getComponent(rootId, Transform.class);
        assertNotNull(rootTransform);
        assertEquals(1f, rootTransform.getPosition().x, 0.001f);
        assertEquals(2f, rootTransform.getPosition().y, 0.001f);
        assertEquals(3f, rootTransform.getPosition().z, 0.001f);

        // Verify hierarchy: root has no parent and has the child
        Hierarchy rootH = registry.getComponent(rootId, Hierarchy.class);
        assertNotNull(rootH);
        assertTrue(rootH.isRoot());
        assertEquals(1, rootH.getChildCount());

        // Verify child has root as parent
        int childId = instance.entity("Child");
        Hierarchy childH = registry.getComponent(childId, Hierarchy.class);
        assertNotNull(childH);
        assertEquals(rootId, childH.getParent());
    }

    @Test
    void rootEntityIsFirstRootNode() {
        GltfNodeData node = new GltfNodeData(
            "OnlyNode", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());

        GltfSceneInstantiator instantiator = new GltfSceneInstantiator(ctx);
        GltfInstance instance = instantiator.instantiate(asset);

        assertEquals(instance.entity("OnlyNode"), instance.rootEntity());
    }

    @Test
    void multipleRootsAreAllMarkedAsRoots() {
        // Scene with two root nodes (common in glTF for multi-object scenes)
        GltfNodeData rootA = new GltfNodeData(
            "RootA", new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of());
        GltfNodeData rootB = new GltfNodeData(
            "RootB", new Vector3f(10, 0, 0), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(rootA, rootB), List.of(0, 1), List.of(), List.of(), List.of(), List.of());

        GltfSceneInstantiator instantiator = new GltfSceneInstantiator(ctx);
        GltfInstance instance = instantiator.instantiate(asset);

        // Both should be root nodes
        int rootAId = instance.entity("RootA");
        int rootBId = instance.entity("RootB");
        Hierarchy hierarchyA = registry.getComponent(rootAId, Hierarchy.class);
        Hierarchy hierarchyB = registry.getComponent(rootBId, Hierarchy.class);
        assertTrue(hierarchyA.isRoot());
        assertTrue(hierarchyB.isRoot());

        // The instance's rootEntity should be the first root node
        assertEquals(rootAId, instance.rootEntity());
    }

    @Test
    void worldTransformIsAddedToAllEntities() {
        GltfNodeData node = new GltfNodeData(
            "Node", new Vector3f(), new Quaternionf(), new Vector3f(2, 2, 2),
            -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());

        GltfSceneInstantiator instantiator = new GltfSceneInstantiator(ctx);
        GltfInstance instance = instantiator.instantiate(asset);

        int nodeId = instance.entity("Node");
        WorldTransform wt = registry.getComponent(nodeId, WorldTransform.class);
        assertNotNull(wt, "WorldTransform should be present on every instantiated entity");
    }

    @Test
    void rotationIsAppliedFromQuaternion() {
        // 90 degrees around Y axis as a quaternion
        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(90));

        GltfNodeData node = new GltfNodeData(
            "Rotated", new Vector3f(0, 0, 0), rotation, new Vector3f(1, 1, 1),
            -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());

        GltfSceneInstantiator instantiator = new GltfSceneInstantiator(ctx);
        GltfInstance instance = instantiator.instantiate(asset);

        int nodeId = instance.entity("Rotated");
        Transform transform = registry.getComponent(nodeId, Transform.class);
        Quaternionf resultQuat = transform.getRotationQuaternion();

        // Verify the quaternion matches (approximately)
        assertEquals(rotation.x, resultQuat.x, 0.001f);
        assertEquals(rotation.y, resultQuat.y, 0.001f);
        assertEquals(rotation.z, resultQuat.z, 0.001f);
        assertEquals(rotation.w, resultQuat.w, 0.001f);
    }

    @Test
    void scaleIsAppliedFromNodeData() {
        GltfNodeData node = new GltfNodeData(
            "Scaled", new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(3, 3, 3),
            -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());

        GltfSceneInstantiator instantiator = new GltfSceneInstantiator(ctx);
        GltfInstance instance = instantiator.instantiate(asset);

        int nodeId = instance.entity("Scaled");
        Transform transform = registry.getComponent(nodeId, Transform.class);
        assertEquals(3f, transform.getScale(), 0.001f);
    }

    @Test
    void deepHierarchyLinksCorrectly() {
        // grandparent -> parent -> child
        GltfNodeData grandparent = new GltfNodeData(
            "Grandparent", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of(1));
        GltfNodeData parent = new GltfNodeData(
            "Parent", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of(2));
        GltfNodeData child = new GltfNodeData(
            "Child", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(grandparent, parent, child), List.of(0), List.of(), List.of(), List.of(), List.of());

        GltfSceneInstantiator instantiator = new GltfSceneInstantiator(ctx);
        GltfInstance instance = instantiator.instantiate(asset);

        int gpId = instance.entity("Grandparent");
        int pId = instance.entity("Parent");
        int cId = instance.entity("Child");

        // Grandparent is root with one child (parent)
        Hierarchy gpH = registry.getComponent(gpId, Hierarchy.class);
        assertTrue(gpH.isRoot());
        assertEquals(1, gpH.getChildCount());
        assertEquals(pId, gpH.getChildren()[0]);

        // Parent has grandparent as parent and child as child
        Hierarchy pH = registry.getComponent(pId, Hierarchy.class);
        assertEquals(gpId, pH.getParent());
        assertEquals(1, pH.getChildCount());
        assertEquals(cId, pH.getChildren()[0]);

        // Child has parent as parent and no children
        Hierarchy cH = registry.getComponent(cId, Hierarchy.class);
        assertEquals(pId, cH.getParent());
        assertEquals(0, cH.getChildCount());
    }

    @Test
    void unnamedNodesAreNotInNameMap() {
        // A node with null name should not appear in the name map
        GltfNodeData unnamed = new GltfNodeData(
            null, new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of());
        GltfNodeData named = new GltfNodeData(
            "Named", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(unnamed, named), List.of(0, 1), List.of(), List.of(), List.of(), List.of());

        GltfSceneInstantiator instantiator = new GltfSceneInstantiator(ctx);
        GltfInstance instance = instantiator.instantiate(asset);

        assertFalse(instance.hasEntity(null != null ? null : "__null__"));
        assertTrue(instance.hasEntity("Named"));
        // The name map should only contain the named node
        assertEquals(1, instance.allEntities().size());
    }

    @Test
    void emptyNameIsNotTracked() {
        // A node with empty string name should not appear in the name map
        GltfNodeData emptyName = new GltfNodeData(
            "", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(emptyName), List.of(0), List.of(), List.of(), List.of(), List.of());

        GltfSceneInstantiator instantiator = new GltfSceneInstantiator(ctx);
        GltfInstance instance = instantiator.instantiate(asset);

        assertFalse(instance.hasEntity(""));
        assertEquals(0, instance.allEntities().size());
    }
}
