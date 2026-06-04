package org.chapzlock.core.asset.gltf;

import java.util.List;
import java.util.stream.IntStream;

import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.animation.TargetPath;
import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.AnimationClipSet;
import org.chapzlock.core.component.AnimationState;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Skeleton;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link GltfAssetManager#instantiate(GltfAsset, int)} (package-private
 * overload).
 *
 * <p>These tests exercise the full five-phase pipeline from a pre-built {@link GltfAsset}
 * through to live ECS entities. GPU phases are skipped because {@code Engine.testEngine()}
 * provides null GPU registries. All test nodes use {@code meshIndex = -1} to avoid GPU
 * operations.
 *
 * <p>The key behavioral contract tested here: all glTF root nodes become children of the
 * provided parent entity. There is no standalone "root" mode.
 */
class GltfAssetManagerInstantiateTest {

    private Engine engine;
    private ComponentRegistry registry;
    private GltfAssetManager gltfAssetManager;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();
        gltfAssetManager = engine.getGltfAssetManager();
    }

    /**
     * Creates a parent entity with Transform and Hierarchy.root() for use in tests.
     */
    private int createParentEntity() {
        int parent = Entity.create(registry);
        registry.addComponents(parent, new Transform(), Hierarchy.root());
        return parent;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Basic hierarchy and transforms
    // ──────────────────────────────────────────────────────────────────────────────

    @Nested
    class BasicHierarchy {

        @Test
        void instantiateCreatesEntitiesForNodes() {
            GltfNodeData root = new GltfNodeData(
                "Root", new Vector3f(1, 2, 3), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of(1));
            GltfNodeData child = new GltfNodeData(
                "Child", new Vector3f(4, 5, 6), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(root, child), List.of(0), List.of(), List.of(), List.of(), List.of());

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            // Verify named entities exist
            assertTrue(registry.hasEntity("Root"));
            assertTrue(registry.hasEntity("Child"));

            // Verify transforms
            int rootId = registry.getEntityByName("Root");
            Transform rootTransform = registry.getComponent(rootId, Transform.class);
            assertNotNull(rootTransform);
            assertEquals(1f, rootTransform.getX(), 0.001f);
            assertEquals(2f, rootTransform.getY(), 0.001f);
            assertEquals(3f, rootTransform.getZ(), 0.001f);

            // Root node is child of parent (not a standalone root)
            Hierarchy rootH = registry.getComponent(rootId, Hierarchy.class);
            assertNotNull(rootH);
            assertEquals(parent, rootH.getParent());
            assertEquals(1, rootH.getChildCount());

            // Child has root as parent
            int childId = registry.getEntityByName("Child");
            Hierarchy childH = registry.getComponent(childId, Hierarchy.class);
            assertNotNull(childH);
            assertEquals(rootId, childH.getParent());
        }

        @Test
        void multipleRootsAreAllChildrenOfParent() {
            GltfNodeData rootA = new GltfNodeData(
                "RootA", new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfNodeData rootB = new GltfNodeData(
                "RootB", new Vector3f(10, 0, 0), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(rootA, rootB), List.of(0, 1), List.of(), List.of(), List.of(), List.of());

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            int rootAId = registry.getEntityByName("RootA");
            int rootBId = registry.getEntityByName("RootB");

            // Both root nodes should be children of parent
            Hierarchy hierarchyA = registry.getComponent(rootAId, Hierarchy.class);
            Hierarchy hierarchyB = registry.getComponent(rootBId, Hierarchy.class);
            assertEquals(parent, hierarchyA.getParent());
            assertEquals(parent, hierarchyB.getParent());

            // Parent should list both as children
            Hierarchy parentH = registry.getComponent(parent, Hierarchy.class);
            assertEquals(2, parentH.getChildCount());
        }

        @Test
        void deepHierarchyLinksCorrectly() {
            GltfNodeData grandparent = new GltfNodeData(
                "Grandparent", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of(1));
            GltfNodeData parentNode = new GltfNodeData(
                "Parent", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of(2));
            GltfNodeData child = new GltfNodeData(
                "Child", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(grandparent, parentNode, child), List.of(0), List.of(), List.of(), List.of(), List.of());

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            int gpId = registry.getEntityByName("Grandparent");
            int pId = registry.getEntityByName("Parent");
            int cId = registry.getEntityByName("Child");

            // Grandparent is child of parent entity, with one child (Parent node)
            Hierarchy gpH = registry.getComponent(gpId, Hierarchy.class);
            assertEquals(parent, gpH.getParent());
            assertEquals(1, gpH.getChildCount());
            assertEquals(pId, gpH.getChildren()[0]);

            // Parent node has grandparent as parent and child as child
            Hierarchy pH = registry.getComponent(pId, Hierarchy.class);
            assertEquals(gpId, pH.getParent());
            assertEquals(1, pH.getChildCount());
            assertEquals(cId, pH.getChildren()[0]);

            // Child has parent node as parent and no children
            Hierarchy cH = registry.getComponent(cId, Hierarchy.class);
            assertEquals(pId, cH.getParent());
            assertEquals(0, cH.getChildCount());
        }

        @Test
        void unnamedNodesAreNotInNameMap() {
            GltfNodeData unnamed = new GltfNodeData(
                null, new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfNodeData named = new GltfNodeData(
                "Named", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(unnamed, named), List.of(0, 1), List.of(), List.of(), List.of(), List.of());

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            assertTrue(registry.hasEntity("Named"));
            assertEquals(-1, registry.getEntityByName("__null__"));
        }

        @Test
        void emptyNameIsNotTracked() {
            GltfNodeData emptyName = new GltfNodeData(
                "", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(emptyName), List.of(0), List.of(), List.of(), List.of(), List.of());

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            assertFalse(registry.hasEntity(""));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Transform properties (rotation, scale)
    // ──────────────────────────────────────────────────────────────────────────────

    @Nested
    class TransformProperties {

        @Test
        void rotationIsAppliedFromQuaternion() {
            Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(90));

            GltfNodeData node = new GltfNodeData(
                "Rotated", new Vector3f(0, 0, 0), rotation, new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            int nodeId = registry.getEntityByName("Rotated");
            Transform transform = registry.getComponent(nodeId, Transform.class);
            Quaternionf resultQuat = new Quaternionf();
            transform.getRotationQuaternion(resultQuat);

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

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            int nodeId = registry.getEntityByName("Scaled");
            Transform transform = registry.getComponent(nodeId, Transform.class);
            Vector3f scale = new Vector3f();
            transform.getScale(scale);
            assertEquals(3f, scale.x, 0.001f);
            assertEquals(3f, scale.y, 0.001f);
            assertEquals(3f, scale.z, 0.001f);
        }

        @Test
        void nodeTransformsArePreservedUnderParent() {
            Vector3f expectedPos = new Vector3f(5, 10, 15);
            GltfNodeData node = new GltfNodeData(
                "Positioned", expectedPos, new Quaternionf(), new Vector3f(2, 2, 2),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            int nodeId = registry.getEntityByName("Positioned");
            Transform transform = registry.getComponent(nodeId, Transform.class);
            assertNotNull(transform);
            assertEquals(expectedPos.x, transform.getX(), 0.001f);
            assertEquals(expectedPos.y, transform.getY(), 0.001f);
            assertEquals(expectedPos.z, transform.getZ(), 0.001f);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Parenting under existing entity
    // ──────────────────────────────────────────────────────────────────────────────

    @Nested
    class ParentingBehavior {

        @Test
        void rootNodesAreChildrenOfProvidedParent() {
            int parent = Entity.create(registry);
            Transform parentTransform = Transform.at(10, 20, 30, 0, 0, 0);
            Hierarchy parentHierarchy = Hierarchy.root();
            registry.addComponents(parent, parentTransform, parentHierarchy);

            GltfNodeData child = new GltfNodeData(
                "Child", new Vector3f(1, 2, 3), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(child), List.of(0), List.of(), List.of(), List.of(), List.of());

            gltfAssetManager.instantiate(asset, parent);

            int childEntity = registry.getEntityByName("Child");
            Hierarchy childHierarchy = registry.getComponent(childEntity, Hierarchy.class);
            assertNotNull(childHierarchy, "Child entity should have a Hierarchy component");
            assertEquals(parent, childHierarchy.getParent(),
                "Child's hierarchy parent should be the provided parent entity");

            int[] parentChildren = parentHierarchy.getChildren();
            boolean found = IntStream.of(parentChildren).anyMatch(id -> id == childEntity);
            assertTrue(found, "Parent hierarchy should contain the child entity");
        }

        @Test
        void createsParentHierarchyIfMissing() {
            // Create a parent entity WITHOUT a Hierarchy component
            int parent = Entity.create(registry);
            registry.addComponent(parent, new Transform());

            GltfNodeData node = new GltfNodeData(
                "Node", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());

            gltfAssetManager.instantiate(asset, parent);

            // Should have created a Hierarchy component for the parent automatically
            Hierarchy parentHierarchy = registry.getComponent(parent, Hierarchy.class);
            assertNotNull(parentHierarchy, "Parent should have a Hierarchy component created for it");
            assertEquals(1, parentHierarchy.getChildCount(),
                "Parent hierarchy should have exactly one child (the glTF root node)");
        }

        @Test
        void multipleRootsAllParentedCorrectly() {
            int parent = createParentEntity();

            GltfNodeData nodeA = new GltfNodeData(
                "RootA", new Vector3f(1, 0, 0), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfNodeData nodeB = new GltfNodeData(
                "RootB", new Vector3f(0, 1, 0), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(nodeA, nodeB), List.of(0, 1), List.of(), List.of(), List.of(), List.of());

            gltfAssetManager.instantiate(asset, parent);

            Hierarchy parentHierarchy = registry.getComponent(parent, Hierarchy.class);
            assertEquals(2, parentHierarchy.getChildCount(),
                "Parent hierarchy should have both glTF root nodes as children");

            int entityA = registry.getEntityByName("RootA");
            int entityB = registry.getEntityByName("RootB");
            assertEquals(parent, registry.getComponent(entityA, Hierarchy.class).getParent());
            assertEquals(parent, registry.getComponent(entityB, Hierarchy.class).getParent());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Animation clips
    // ──────────────────────────────────────────────────────────────────────────────

    @Nested
    class AnimationClips {

        @Test
        void createsAnimationClipSetOnParentEntity() {
            GltfNodeData root = new GltfNodeData(
                "Root", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of(1));
            GltfNodeData bone = new GltfNodeData(
                "Bone", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAnimationData.ChannelData channel = new GltfAnimationData.ChannelData(
                1, "translation", "LINEAR",
                new float[]{0f, 1f},
                new float[]{0, 0, 0, 10, 20, 30}
            );
            GltfAnimationData animData = new GltfAnimationData("Walk", 1f, List.of(channel));

            GltfAsset asset = new GltfAsset(
                List.of(root, bone), List.of(0), List.of(), List.of(), List.of(), List.of(animData));

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            // AnimationClipSet should be on the parent entity
            AnimationClipSet clipSet = registry.getComponent(parent, AnimationClipSet.class);
            assertNotNull(clipSet);
            assertEquals(1, clipSet.size());

            Animation walk = clipSet.get("Walk");
            assertNotNull(walk);
            assertEquals(1f, walk.duration());
            assertEquals(1, walk.channels().size());

            // Channel target should be the bone entity
            int boneEntity = registry.getEntityByName("Bone");
            assertEquals(boneEntity, walk.channels().get(0).targetEntity());
            assertEquals(TargetPath.TRANSLATION, walk.channels().get(0).path());
        }

        @Test
        void createsAnimationStateOnParentEntity() {
            GltfNodeData root = new GltfNodeData(
                "Root", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAnimationData.ChannelData channel = new GltfAnimationData.ChannelData(
                0, "rotation", "LINEAR",
                new float[]{0f, 1f},
                new float[]{0, 0, 0, 1, 0, 0.707f, 0, 0.707f}
            );
            GltfAnimationData animData = new GltfAnimationData("Spin", 1f, List.of(channel));

            GltfAsset asset = new GltfAsset(
                List.of(root), List.of(0), List.of(), List.of(), List.of(), List.of(animData));

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            AnimationState state = registry.getComponent(parent, AnimationState.class);
            assertNotNull(state);
            assertFalse(state.isPlaying());
        }

        @Test
        void noAnimationsProducesNoClipSet() {
            GltfNodeData root = new GltfNodeData(
                "Root", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(root), List.of(0), List.of(), List.of(), List.of(), List.of());

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            assertNull(registry.getComponent(parent, AnimationClipSet.class));
            assertNull(registry.getComponent(parent, AnimationState.class));
        }

        @Test
        void animationClipSetAttachedToParentNotGltfRootNode() {
            int parent = createParentEntity();

            GltfNodeData node = new GltfNodeData(
                "Node", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAnimationData.ChannelData ch = new GltfAnimationData.ChannelData(
                0, "translation", "LINEAR",
                new float[]{0f, 1f},
                new float[]{0, 0, 0, 1, 2, 3}
            );
            GltfAnimationData anim = new GltfAnimationData("Idle", 1f, List.of(ch));

            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of(anim));

            gltfAssetManager.instantiate(asset, parent);

            // AnimationClipSet should be on the parent entity (not on the glTF node)
            AnimationClipSet clips = registry.getComponent(parent, AnimationClipSet.class);
            assertNotNull(clips, "AnimationClipSet should be attached to the parent entity");
            assertNotNull(clips.get("Idle"), "The 'Idle' clip should be present in the clip set");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Skeleton creation
    // ──────────────────────────────────────────────────────────────────────────────

    @Nested
    class SkeletonCreation {

        @Test
        void createsSkeletonOnSkinnedNode() {
            GltfNodeData skinNode = new GltfNodeData(
                "Mesh", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, 0, List.of());
            GltfNodeData jointA = new GltfNodeData(
                "JointA", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfNodeData jointB = new GltfNodeData(
                "JointB", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            Matrix4f ibmA = new Matrix4f().identity();
            Matrix4f ibmB = new Matrix4f().translate(1, 0, 0);

            GltfSkinData skin = new GltfSkinData("Armature", new int[]{1, 2}, new Matrix4f[]{ibmA, ibmB});

            GltfAsset asset = new GltfAsset(
                List.of(skinNode, jointA, jointB), List.of(0),
                List.of(), List.of(), List.of(skin), List.of());

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            int meshEntityId = registry.getEntityByName("Mesh");
            Skeleton skeleton = registry.getComponent(meshEntityId, Skeleton.class);

            assertNotNull(skeleton);
            assertEquals(2, skeleton.jointCount());
            int jointAEntity = registry.getEntityByName("JointA");
            int jointBEntity = registry.getEntityByName("JointB");
            assertEquals(jointAEntity, skeleton.getJointEntities()[0]);
            assertEquals(jointBEntity, skeleton.getJointEntities()[1]);
        }

        @Test
        void nodeWithoutSkinGetsNoSkeleton() {
            GltfNodeData node = new GltfNodeData(
                "Box", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());

            int parent = createParentEntity();
            gltfAssetManager.instantiate(asset, parent);

            int entityId = registry.getEntityByName("Box");
            assertNull(registry.getComponent(entityId, Skeleton.class));
        }
    }
}
