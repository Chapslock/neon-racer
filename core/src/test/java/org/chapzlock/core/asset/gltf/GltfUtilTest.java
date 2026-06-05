package org.chapzlock.core.asset.gltf;

import java.util.List;

import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.animation.Interpolation;
import org.chapzlock.core.animation.TargetPath;
import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.AnimationClipSet;
import org.chapzlock.core.component.AnimationState;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Name;
import org.chapzlock.core.component.Skeleton;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.ecs.EntityFactory;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GltfUtil} static phase methods.
 *
 * <p>Tests cover the non-GPU phases (1, 2, 4, 5) since {@code Engine.testEngine()} provides
 * null GPU registries. Phase 3 (uploadMeshesAndMaterials) requires GPU systems and is not
 * tested here.
 */
class GltfUtilTest {

    private Engine engine;
    private ComponentRegistry registry;
    private EntityFactory entityFactory;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();
        entityFactory = engine.getEntityFactory();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Phase 1: createNodes
    // ──────────────────────────────────────────────────────────────────────────────

    @Nested
    class CreateNodes {

        @Test
        void createsEntityPerNode() {
            GltfAsset asset = assetWithNodes(
                node("A", 0, 0, 0),
                node("B", 1, 2, 3)
            );
            int parent = Entity.create(registry);

            int[] ids = GltfUtil.createNodes(asset, parent, registry);

            assertEquals(2, ids.length);
            assertTrue(ids[0] > 0);
            assertTrue(ids[1] > 0);
            assertNotEquals(ids[0], ids[1]);
        }

        @Test
        void setsTransformFromNodeTRS() {
            Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(90));
            GltfNodeData node = new GltfNodeData(
                "Rotated", new Vector3f(1, 2, 3), rotation, new Vector3f(2, 3, 4),
                -1, -1, List.of());
            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());
            int parent = Entity.create(registry);

            int[] ids = GltfUtil.createNodes(asset, parent, registry);

            Transform transform = registry.getComponent(ids[0], Transform.class);
            assertNotNull(transform);
            assertEquals(1f, transform.getX(), 0.001f);
            assertEquals(2f, transform.getY(), 0.001f);
            assertEquals(3f, transform.getZ(), 0.001f);

            Vector3f scale = new Vector3f();
            transform.getScale(scale);
            assertEquals(2f, scale.x, 0.001f);
            assertEquals(3f, scale.y, 0.001f);
            assertEquals(4f, scale.z, 0.001f);

            Quaternionf resultQuat = new Quaternionf();
            transform.getRotationQuaternion(resultQuat);
            assertEquals(rotation.x, resultQuat.x, 0.001f);
            assertEquals(rotation.y, resultQuat.y, 0.001f);
            assertEquals(rotation.z, resultQuat.z, 0.001f);
            assertEquals(rotation.w, resultQuat.w, 0.001f);
        }

        @Test
        void addsNameComponentForNamedNodes() {
            GltfAsset asset = assetWithNodes(node("Player", 0, 0, 0));
            int parent = Entity.create(registry);

            int[] ids = GltfUtil.createNodes(asset, parent, registry);

            Name name = registry.getComponent(ids[0], Name.class);
            assertNotNull(name);
            assertEquals("Player", name.getValue());
        }

        @Test
        void skipsNameForNullName() {
            GltfNodeData unnamed = new GltfNodeData(
                null, new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAsset asset = new GltfAsset(
                List.of(unnamed), List.of(0), List.of(), List.of(), List.of(), List.of());
            int parent = Entity.create(registry);

            int[] ids = GltfUtil.createNodes(asset, parent, registry);

            assertNull(registry.getComponent(ids[0], Name.class));
        }

        @Test
        void skipsNameForEmptyName() {
            GltfNodeData emptyName = new GltfNodeData(
                "", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAsset asset = new GltfAsset(
                List.of(emptyName), List.of(0), List.of(), List.of(), List.of(), List.of());
            int parent = Entity.create(registry);

            int[] ids = GltfUtil.createNodes(asset, parent, registry);

            assertNull(registry.getComponent(ids[0], Name.class));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Phase 2: wireHierarchy
    // ──────────────────────────────────────────────────────────────────────────────

    @Nested
    class WireHierarchy {

        @Test
        void rootNodesBecomeChildrenOfParent() {
            GltfAsset asset = assetWithNodes(node("Root", 0, 0, 0));
            int parent = Entity.create(registry);
            registry.addComponent(parent, new Transform());
            registry.addComponent(parent, Hierarchy.root());

            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.wireHierarchy(asset, ids, parent, entityFactory);

            Hierarchy rootH = registry.getComponent(ids[0], Hierarchy.class);
            assertNotNull(rootH);
            assertEquals(parent, rootH.getParent());
        }

        @Test
        void parentHierarchyGetsRootChildren() {
            GltfAsset asset = assetWithNodes(node("Root", 0, 0, 0));
            int parent = Entity.create(registry);
            registry.addComponent(parent, new Transform());
            Hierarchy parentH = Hierarchy.root();
            registry.addComponent(parent, parentH);

            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.wireHierarchy(asset, ids, parent, entityFactory);

            int[] children = parentH.getChildren();
            assertEquals(1, children.length);
            assertEquals(ids[0], children[0]);
        }

        @Test
        void createsParentHierarchyIfMissing() {
            GltfAsset asset = assetWithNodes(node("Root", 0, 0, 0));
            int parent = Entity.create(registry);
            registry.addComponent(parent, new Transform());

            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.wireHierarchy(asset, ids, parent, entityFactory);

            Hierarchy parentH = registry.getComponent(parent, Hierarchy.class);
            assertNotNull(parentH, "Should create Hierarchy for parent if missing");
            assertTrue(parentH.isRoot());
            assertEquals(1, parentH.getChildCount());
        }

        @Test
        void nonRootNodesGetParentFromGltfTree() {
            GltfNodeData parentNode = new GltfNodeData(
                "Parent", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of(1));
            GltfNodeData childNode = new GltfNodeData(
                "Child", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAsset asset = new GltfAsset(
                List.of(parentNode, childNode), List.of(0),
                List.of(), List.of(), List.of(), List.of());

            int anchor = Entity.create(registry);
            registry.addComponent(anchor, new Transform());

            int[] ids = GltfUtil.createNodes(asset, anchor, registry);
            GltfUtil.wireHierarchy(asset, ids, anchor, entityFactory);

            // Child should point to the glTF parent node entity
            Hierarchy childH = registry.getComponent(ids[1], Hierarchy.class);
            assertNotNull(childH);
            assertEquals(ids[0], childH.getParent());

            // Parent should list child in its children
            Hierarchy parentH = registry.getComponent(ids[0], Hierarchy.class);
            assertNotNull(parentH);
            assertEquals(1, parentH.getChildCount());
            assertEquals(ids[1], parentH.getChildren()[0]);
        }

        @Test
        void deepHierarchyLinksCorrectly() {
            // grandparent -> parent -> child
            GltfNodeData grandparent = new GltfNodeData(
                "GP", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of(1));
            GltfNodeData parent = new GltfNodeData(
                "P", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of(2));
            GltfNodeData child = new GltfNodeData(
                "C", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAsset asset = new GltfAsset(
                List.of(grandparent, parent, child), List.of(0),
                List.of(), List.of(), List.of(), List.of());

            int anchor = Entity.create(registry);
            registry.addComponent(anchor, new Transform());

            int[] ids = GltfUtil.createNodes(asset, anchor, registry);
            GltfUtil.wireHierarchy(asset, ids, anchor, entityFactory);

            // GP is child of anchor
            Hierarchy gpH = registry.getComponent(ids[0], Hierarchy.class);
            assertEquals(anchor, gpH.getParent());
            assertEquals(1, gpH.getChildCount());
            assertEquals(ids[1], gpH.getChildren()[0]);

            // P is child of GP
            Hierarchy pH = registry.getComponent(ids[1], Hierarchy.class);
            assertEquals(ids[0], pH.getParent());
            assertEquals(1, pH.getChildCount());
            assertEquals(ids[2], pH.getChildren()[0]);

            // C is child of P
            Hierarchy cH = registry.getComponent(ids[2], Hierarchy.class);
            assertEquals(ids[1], cH.getParent());
            assertEquals(0, cH.getChildCount());
        }

        @Test
        void multipleRootsAllBecomeChildrenOfParent() {
            GltfNodeData rootA = new GltfNodeData(
                "A", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfNodeData rootB = new GltfNodeData(
                "B", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAsset asset = new GltfAsset(
                List.of(rootA, rootB), List.of(0, 1),
                List.of(), List.of(), List.of(), List.of());

            int anchor = Entity.create(registry);
            registry.addComponent(anchor, new Transform());
            Hierarchy anchorH = Hierarchy.root();
            registry.addComponent(anchor, anchorH);

            int[] ids = GltfUtil.createNodes(asset, anchor, registry);
            GltfUtil.wireHierarchy(asset, ids, anchor, entityFactory);

            // Both should be children of anchor
            assertEquals(anchor, registry.getComponent(ids[0], Hierarchy.class).getParent());
            assertEquals(anchor, registry.getComponent(ids[1], Hierarchy.class).getParent());
            assertEquals(2, anchorH.getChildCount());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Phase 4: createSkeletons
    // ──────────────────────────────────────────────────────────────────────────────

    @Nested
    class CreateSkeletons {

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

            int parent = Entity.create(registry);
            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.createSkeletons(asset, ids, registry);

            Skeleton skeleton = registry.getComponent(ids[0], Skeleton.class);
            assertNotNull(skeleton);
            assertEquals(2, skeleton.jointCount());
            assertEquals(ids[1], skeleton.getJointEntities()[0]);
            assertEquals(ids[2], skeleton.getJointEntities()[1]);
        }

        @Test
        void noSkeletonForNodeWithoutSkin() {
            GltfNodeData node = new GltfNodeData(
                "Box", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());

            int parent = Entity.create(registry);
            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.createSkeletons(asset, ids, registry);

            assertNull(registry.getComponent(ids[0], Skeleton.class));
        }

        @Test
        void skinIndexOutOfBoundsIsSkipped() {
            // Node references skin index 5, but there are no skins
            GltfNodeData node = new GltfNodeData(
                "Bad", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, 5, List.of());
            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of());

            int parent = Entity.create(registry);
            int[] ids = GltfUtil.createNodes(asset, parent, registry);

            // Should not throw
            GltfUtil.createSkeletons(asset, ids, registry);
            assertNull(registry.getComponent(ids[0], Skeleton.class));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Phase 5: createAnimations
    // ──────────────────────────────────────────────────────────────────────────────

    @Nested
    class CreateAnimations {

        @Test
        void createsClipSetOnParentEntity() {
            GltfNodeData bone = new GltfNodeData(
                "Bone", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());

            GltfAnimationData.ChannelData channel = new GltfAnimationData.ChannelData(
                0, "translation", "LINEAR",
                new float[]{0f, 1f},
                new float[]{0, 0, 0, 10, 20, 30});
            GltfAnimationData animData = new GltfAnimationData("Walk", 1f, List.of(channel));

            GltfAsset asset = new GltfAsset(
                List.of(bone), List.of(0), List.of(), List.of(), List.of(), List.of(animData));

            int parent = Entity.create(registry);
            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.createAnimations(asset, ids, parent, registry);

            AnimationClipSet clipSet = registry.getComponent(parent, AnimationClipSet.class);
            assertNotNull(clipSet);
            assertEquals(1, clipSet.size());

            Animation walk = clipSet.get("Walk");
            assertNotNull(walk);
            assertEquals(1f, walk.duration());
            assertEquals(1, walk.channels().size());
            assertEquals(ids[0], walk.channels().get(0).targetEntity());
            assertEquals(TargetPath.TRANSLATION, walk.channels().get(0).path());
        }

        @Test
        void createsAnimationStateOnParent() {
            GltfNodeData node = new GltfNodeData(
                "Node", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAnimationData.ChannelData channel = new GltfAnimationData.ChannelData(
                0, "rotation", "LINEAR",
                new float[]{0f, 1f},
                new float[]{0, 0, 0, 1, 0, 0.707f, 0, 0.707f});
            GltfAnimationData animData = new GltfAnimationData("Spin", 1f, List.of(channel));
            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of(animData));

            int parent = Entity.create(registry);
            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.createAnimations(asset, ids, parent, registry);

            AnimationState state = registry.getComponent(parent, AnimationState.class);
            assertNotNull(state);
            assertFalse(state.isPlaying());
        }

        @Test
        void noAnimationsProducesNoComponents() {
            GltfAsset asset = assetWithNodes(node("Node", 0, 0, 0));

            int parent = Entity.create(registry);
            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.createAnimations(asset, ids, parent, registry);

            assertNull(registry.getComponent(parent, AnimationClipSet.class));
            assertNull(registry.getComponent(parent, AnimationState.class));
        }

        @Test
        void multipleClipsAreAllPresent() {
            GltfNodeData node = new GltfNodeData(
                "Node", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAnimationData.ChannelData ch = new GltfAnimationData.ChannelData(
                0, "translation", "LINEAR",
                new float[]{0f, 1f}, new float[]{0, 0, 0, 1, 0, 0});
            GltfAnimationData walk = new GltfAnimationData("Walk", 1f, List.of(ch));
            GltfAnimationData run = new GltfAnimationData("Run", 0.5f, List.of(ch));
            GltfAnimationData idle = new GltfAnimationData("Idle", 2f, List.of(ch));
            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(),
                List.of(walk, run, idle));

            int parent = Entity.create(registry);
            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.createAnimations(asset, ids, parent, registry);

            AnimationClipSet clipSet = registry.getComponent(parent, AnimationClipSet.class);
            assertEquals(3, clipSet.size());
            assertNotNull(clipSet.get("Walk"));
            assertNotNull(clipSet.get("Run"));
            assertNotNull(clipSet.get("Idle"));
        }

        @Test
        void unnamedAnimationGetsGeneratedName() {
            GltfNodeData node = new GltfNodeData(
                "Node", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAnimationData.ChannelData ch = new GltfAnimationData.ChannelData(
                0, "scale", "STEP",
                new float[]{0f}, new float[]{1, 1, 1});
            GltfAnimationData unnamed = new GltfAnimationData(null, 0.5f, List.of(ch));
            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of(unnamed));

            int parent = Entity.create(registry);
            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.createAnimations(asset, ids, parent, registry);

            AnimationClipSet clipSet = registry.getComponent(parent, AnimationClipSet.class);
            assertNotNull(clipSet.get("animation_0"));
        }

        @Test
        void channelInterpolationTypesAreMapped() {
            GltfNodeData node = new GltfNodeData(
                "Node", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAnimationData.ChannelData linear = new GltfAnimationData.ChannelData(
                0, "translation", "LINEAR", new float[]{0f, 1f}, new float[]{0, 0, 0, 1, 0, 0});
            GltfAnimationData.ChannelData step = new GltfAnimationData.ChannelData(
                0, "rotation", "STEP", new float[]{0f, 1f}, new float[]{0, 0, 0, 1, 0, 0, 0, 1});
            GltfAnimationData.ChannelData cubic = new GltfAnimationData.ChannelData(
                0, "scale", "CUBICSPLINE", new float[]{0f, 1f},
                new float[]{0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 2, 2, 2, 0, 0, 0});
            GltfAnimationData anim = new GltfAnimationData("Mix", 1f, List.of(linear, step, cubic));
            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of(anim));

            int parent = Entity.create(registry);
            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.createAnimations(asset, ids, parent, registry);

            Animation clip = registry.getComponent(parent, AnimationClipSet.class).get("Mix");
            assertEquals(Interpolation.LINEAR, clip.channels().get(0).interpolation());
            assertEquals(Interpolation.STEP, clip.channels().get(1).interpolation());
            assertEquals(Interpolation.CUBICSPLINE, clip.channels().get(2).interpolation());
        }

        @Test
        void unknownPathIsSkipped() {
            GltfNodeData node = new GltfNodeData(
                "Node", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1),
                -1, -1, List.of());
            GltfAnimationData.ChannelData bad = new GltfAnimationData.ChannelData(
                0, "unknown_path", "LINEAR", new float[]{0f}, new float[]{0});
            GltfAnimationData.ChannelData good = new GltfAnimationData.ChannelData(
                0, "translation", "LINEAR", new float[]{0f, 1f}, new float[]{0, 0, 0, 1, 0, 0});
            GltfAnimationData anim = new GltfAnimationData("Clip", 1f, List.of(bad, good));
            GltfAsset asset = new GltfAsset(
                List.of(node), List.of(0), List.of(), List.of(), List.of(), List.of(anim));

            int parent = Entity.create(registry);
            int[] ids = GltfUtil.createNodes(asset, parent, registry);
            GltfUtil.createAnimations(asset, ids, parent, registry);

            Animation clip = registry.getComponent(parent, AnimationClipSet.class).get("Clip");
            // Only the valid channel should remain
            assertEquals(1, clip.channels().size());
            assertEquals(TargetPath.TRANSLATION, clip.channels().get(0).path());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Test helpers
    // ──────────────────────────────────────────────────────────────────────────────

    private static GltfNodeData node(String name, float x, float y, float z) {
        return new GltfNodeData(
            name, new Vector3f(x, y, z), new Quaternionf(), new Vector3f(1, 1, 1),
            -1, -1, List.of());
    }

    private static GltfAsset assetWithNodes(GltfNodeData... nodes) {
        List<Integer> rootIndices = new java.util.ArrayList<>();
        for (int i = 0; i < nodes.length; i++) {
            rootIndices.add(i);
        }
        return new GltfAsset(
            List.of(nodes), rootIndices, List.of(), List.of(), List.of(), List.of());
    }
}
