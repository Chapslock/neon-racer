package org.chapzlock.core.scene;

import java.util.List;

import org.chapzlock.core.ecs.GameSystem;

/**
 * Holds the runtime state of a loaded scene: its entities, tracked GPU assets, and systems.
 *
 * <p><strong>What this represents:</strong> A {@link Scene} is a blueprint (an immutable
 * description of what to create). A {@code SceneInstance} is the live result of instantiating
 * that blueprint — the actual entities in the ECS, the GPU resources they use, and the
 * systems running their logic.
 *
 * <p><strong>Lifecycle:</strong>
 * <ol>
 *   <li>Created by {@link SceneInstantiator#instantiate(Scene)} when a scene is loaded</li>
 *   <li>Stored by {@link SceneManager} for the scene's lifetime</li>
 *   <li>Passed to {@link SceneInstantiator#teardown(SceneInstance)} when the scene is unloaded</li>
 * </ol>
 *
 * <p><strong>Why a record?</strong> Scene instances are immutable once created — their contents
 * are fixed at instantiation time and never change. A record provides value semantics,
 * automatic {@code equals}/{@code hashCode}, and clear intent that this is purely data.
 *
 * @param scene     the original scene blueprint that was instantiated
 * @param entityIds all entity IDs created for this scene (destroyed on unload)
 * @param assets    GPU resource IDs retained for this scene (released on unload)
 * @param systems   initialized game systems for this scene (destroyed on unload)
 */
public record SceneInstance(
    Scene scene,
    List<Integer> entityIds,
    SceneAssets assets,
    List<GameSystem> systems
) {}
