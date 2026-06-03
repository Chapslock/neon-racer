package org.chapzlock.core.physics;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.RawMeshData;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.render.MeshSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollisionShapeResolverHierarchyTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private MeshSystem meshSystem;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        meshSystem = mock(MeshSystem.class);
    }

    @Test
    void resolveMeshData_findsMeshOnChildEntities() {
        int parent = Entity.create(registry);
        registry.addComponent(parent, Transform.at(0, 0, 0));

        int child = Entity.create(registry);
        registry.addComponent(child, Transform.at(0, 0, 0));
        Mesh childMesh = Mesh.builder().id(42).build();
        registry.addComponent(child, childMesh);

        Hierarchy parentHierarchy = Hierarchy.root();
        parentHierarchy.addChild(child);
        registry.addComponent(parent, parentHierarchy);

        RawMeshData meshData = RawMeshData.builder()
            .positions(new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0})
            .texCoords(new float[]{0, 0, 1, 0, 0, 1})
            .indices(new int[]{0, 1, 2})
            .build();
        when(meshSystem.getRawMeshById(42)).thenReturn(meshData);

        RawMeshData result = PhysicsWorld.resolveMeshData(parent, registry, meshSystem);

        assertNotNull(result);
        assertEquals(9, result.getPositions().length);
    }

    @Test
    void resolveMeshData_prefersOwnMeshOverChildren() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, Mesh.builder().id(10).build());

        RawMeshData ownData = RawMeshData.builder()
            .positions(new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0})
            .texCoords(new float[]{0, 0, 1, 0, 0, 1})
            .indices(new int[]{0, 1, 2})
            .build();
        when(meshSystem.getRawMeshById(10)).thenReturn(ownData);

        RawMeshData result = PhysicsWorld.resolveMeshData(entity, registry, meshSystem);

        assertSame(ownData, result);
    }

    @Test
    void resolveMeshData_mergesMultipleChildMeshes() {
        int parent = Entity.create(registry);
        registry.addComponent(parent, Transform.at(0, 0, 0));

        int child1 = Entity.create(registry);
        registry.addComponent(child1, Mesh.builder().id(1).build());
        int child2 = Entity.create(registry);
        registry.addComponent(child2, Mesh.builder().id(2).build());

        Hierarchy parentHierarchy = Hierarchy.root();
        parentHierarchy.addChild(child1);
        parentHierarchy.addChild(child2);
        registry.addComponent(parent, parentHierarchy);

        RawMeshData data1 = RawMeshData.builder()
            .positions(new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0})
            .texCoords(new float[]{0, 0, 1, 0, 0, 1})
            .indices(new int[]{0, 1, 2})
            .build();
        RawMeshData data2 = RawMeshData.builder()
            .positions(new float[]{2, 0, 0, 3, 0, 0, 2, 1, 0})
            .texCoords(new float[]{0, 0, 1, 0, 0, 1})
            .indices(new int[]{0, 1, 2})
            .build();
        when(meshSystem.getRawMeshById(1)).thenReturn(data1);
        when(meshSystem.getRawMeshById(2)).thenReturn(data2);

        RawMeshData result = PhysicsWorld.resolveMeshData(parent, registry, meshSystem);

        assertNotNull(result);
        assertEquals(18, result.getPositions().length);
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5}, result.getIndices());
    }

    @Test
    void resolveMeshData_findsGrandchildMesh() {
        // Simulates a character model: parent → glTF root → mesh node
        int parent = Entity.create(registry);
        registry.addComponent(parent, Transform.at(0, 0, 0));

        int child = Entity.create(registry);
        registry.addComponent(child, Transform.at(0, 0, 0));

        int grandchild = Entity.create(registry);
        registry.addComponent(grandchild, Transform.at(0, 0, 0));
        registry.addComponent(grandchild, Mesh.builder().id(99).build());

        Hierarchy childHierarchy = Hierarchy.childOf(parent);
        childHierarchy.addChild(grandchild);
        registry.addComponent(child, childHierarchy);

        Hierarchy parentHierarchy = Hierarchy.root();
        parentHierarchy.addChild(child);
        registry.addComponent(parent, parentHierarchy);

        registry.addComponent(grandchild, Hierarchy.childOf(child));

        RawMeshData meshData = RawMeshData.builder()
            .positions(new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0})
            .texCoords(new float[]{0, 0, 1, 0, 0, 1})
            .indices(new int[]{0, 1, 2})
            .build();
        when(meshSystem.getRawMeshById(99)).thenReturn(meshData);

        RawMeshData result = PhysicsWorld.resolveMeshData(parent, registry, meshSystem);

        assertNotNull(result, "Should find mesh on grandchild via recursive traversal");
        assertEquals(9, result.getPositions().length);
    }

    @Test
    void resolveMeshData_findsMeshesAtMultipleLevels() {
        // parent → child1 (has mesh) → grandchild (has mesh)
        int parent = Entity.create(registry);
        registry.addComponent(parent, Transform.at(0, 0, 0));

        int child = Entity.create(registry);
        registry.addComponent(child, Transform.at(0, 0, 0));
        registry.addComponent(child, Mesh.builder().id(10).build());

        int grandchild = Entity.create(registry);
        registry.addComponent(grandchild, Transform.at(0, 0, 0));
        registry.addComponent(grandchild, Mesh.builder().id(20).build());

        Hierarchy childHierarchy = Hierarchy.childOf(parent);
        childHierarchy.addChild(grandchild);
        registry.addComponent(child, childHierarchy);

        Hierarchy parentHierarchy = Hierarchy.root();
        parentHierarchy.addChild(child);
        registry.addComponent(parent, parentHierarchy);

        registry.addComponent(grandchild, Hierarchy.childOf(child));

        RawMeshData data1 = RawMeshData.builder()
            .positions(new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0})
            .texCoords(new float[]{0, 0, 1, 0, 0, 1})
            .indices(new int[]{0, 1, 2})
            .build();
        RawMeshData data2 = RawMeshData.builder()
            .positions(new float[]{2, 0, 0, 3, 0, 0, 2, 1, 0})
            .texCoords(new float[]{0, 0, 1, 0, 0, 1})
            .indices(new int[]{0, 1, 2})
            .build();
        when(meshSystem.getRawMeshById(10)).thenReturn(data1);
        when(meshSystem.getRawMeshById(20)).thenReturn(data2);

        RawMeshData result = PhysicsWorld.resolveMeshData(parent, registry, meshSystem);

        assertNotNull(result, "Should find meshes at child and grandchild levels");
        assertEquals(18, result.getPositions().length);
    }

    @Test
    void resolveMeshData_returnsNull_whenNoMeshAnywhere() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));

        RawMeshData result = PhysicsWorld.resolveMeshData(entity, registry, meshSystem);

        assertNull(result);
    }
}
