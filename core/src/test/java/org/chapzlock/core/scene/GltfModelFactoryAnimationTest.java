package org.chapzlock.core.scene;

import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.animation.TargetPath;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.gltf.*;
import org.chapzlock.core.component.AnimationClipSet;
import org.chapzlock.core.component.AnimationState;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GltfModelFactoryAnimationTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private GltfModelFactory instantiator;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        instantiator = new GltfModelFactory(ctx);
    }

    @Test
    void phase5CreatesAnimationClipSetOnRoot() {
        GltfNodeData root = new GltfNodeData("Root", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), -1, -1, List.of(1));
        GltfNodeData bone = new GltfNodeData("Bone", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), -1, -1, List.of());

        GltfAnimationData.ChannelData channel = new GltfAnimationData.ChannelData(
            1, "translation", "LINEAR",
            new float[]{0f, 1f},
            new float[]{0, 0, 0, 10, 20, 30}
        );
        GltfAnimationData animData = new GltfAnimationData("Walk", 1f, List.of(channel));

        GltfAsset asset = new GltfAsset(
            List.of(root, bone),
            List.of(0),
            List.of(),
            List.of(),
            List.of(),
            List.of(animData)
        );

        GltfInstance instance = instantiator.instantiate(asset);
        int rootEntity = instance.rootEntity();

        AnimationClipSet clipSet = registry.getComponent(rootEntity, AnimationClipSet.class);
        assertNotNull(clipSet);
        assertEquals(1, clipSet.size());

        Animation walk = clipSet.get("Walk");
        assertNotNull(walk);
        assertEquals(1f, walk.duration());
        assertEquals(1, walk.channels().size());

        int boneEntity = instance.entity("Bone");
        assertEquals(boneEntity, walk.channels().get(0).targetEntity());
        assertEquals(TargetPath.TRANSLATION, walk.channels().get(0).path());
    }

    @Test
    void phase5CreatesAnimationStateOnRoot() {
        GltfNodeData root = new GltfNodeData("Root", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), -1, -1, List.of());

        GltfAnimationData.ChannelData channel = new GltfAnimationData.ChannelData(
            0, "rotation", "LINEAR",
            new float[]{0f, 1f},
            new float[]{0, 0, 0, 1, 0, 0.707f, 0, 0.707f}
        );
        GltfAnimationData animData = new GltfAnimationData("Spin", 1f, List.of(channel));

        GltfAsset asset = new GltfAsset(
            List.of(root),
            List.of(0),
            List.of(),
            List.of(),
            List.of(),
            List.of(animData)
        );

        GltfInstance instance = instantiator.instantiate(asset);
        int rootEntity = instance.rootEntity();

        AnimationState state = registry.getComponent(rootEntity, AnimationState.class);
        assertNotNull(state);
        assertFalse(state.isPlaying());
    }

    @Test
    void noAnimationsProducesNoClipSet() {
        GltfNodeData root = new GltfNodeData("Root", new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), -1, -1, List.of());

        GltfAsset asset = new GltfAsset(
            List.of(root),
            List.of(0),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        GltfInstance instance = instantiator.instantiate(asset);
        int rootEntity = instance.rootEntity();

        assertNull(registry.getComponent(rootEntity, AnimationClipSet.class));
        assertNull(registry.getComponent(rootEntity, AnimationState.class));
    }
}
