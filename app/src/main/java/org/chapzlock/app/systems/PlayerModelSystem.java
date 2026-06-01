package org.chapzlock.app.systems;

import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.component.WorldTransform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityView;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.component.AnimationClipSet;
import org.chapzlock.core.component.AnimationState;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.scene.GltfInstance;
import org.chapzlock.core.scene.SceneManager;

import java.util.List;

/**
 * Loads the player character's 3D model (glTF) and attaches it as a child of the player entity.
 *
 * <p>This system runs once during {@link #onInit(EngineContext)} to load the humanoid character
 * model from {@code glb/player-character.glb}. The loaded model's root entity is parented to the
 * player entity (identified by {@link PlayerTag}), so the model moves and rotates with the player.
 *
 * <p><strong>Why a separate system?</strong> The scene builder's {@code fromGltf()} loads one
 * file as the entire environment. Individual entity models need to be loaded separately via
 * {@link SceneManager#loadGltf(String)} and manually parented into the scene hierarchy.
 */
public class PlayerModelSystem implements GameSystem {

    private static final String PLAYER_MODEL_PATH = "glb/player-character.glb";

    private ComponentRegistry registry;

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        SceneManager sceneManager = ctx.getSceneManager();

        List<EntityView> players = registry.view(PlayerTag.class, Transform.class);
        if (players.isEmpty()) {
            return;
        }

        int playerEntity = players.get(0).getId();

        if (registry.getComponent(playerEntity, WorldTransform.class) == null) {
            registry.addComponent(playerEntity, new WorldTransform());
        }

        GltfInstance playerModel = sceneManager.loadGltf(PLAYER_MODEL_PATH);
        int modelRoot = playerModel.rootEntity();

        Hierarchy playerHierarchy = registry.getComponent(playerEntity, Hierarchy.class);
        if (playerHierarchy == null) {
            playerHierarchy = Hierarchy.root();
            registry.addComponent(playerEntity, playerHierarchy);
        }
        playerHierarchy.addChild(modelRoot);

        Hierarchy modelHierarchy = registry.getComponent(modelRoot, Hierarchy.class);
        if (modelHierarchy == null) {
            modelHierarchy = Hierarchy.childOf(playerEntity);
            registry.addComponent(modelRoot, modelHierarchy);
        } else {
            modelHierarchy.setParent(playerEntity);
        }

        AnimationClipSet clipSet = registry.getComponent(modelRoot, AnimationClipSet.class);
        AnimationState state = registry.getComponent(modelRoot, AnimationState.class);
        if (clipSet != null && state != null && !clipSet.clipNames().isEmpty()) {
            String firstName = clipSet.clipNames().iterator().next();
            Animation clip = clipSet.get(firstName);
            state.play(clip);
            state.setLooping(true);
            Log.debug("Playing animation: \"" + firstName + "\"");
        }
    }

    @Override
    public void onUpdate(float deltaTime) {
        // No per-frame work — model is attached at init and moves with the player via hierarchy.
    }
}
