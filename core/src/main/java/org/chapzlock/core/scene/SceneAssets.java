package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks GPU resource IDs that a scene has retained during instantiation.
 *
 * <p><strong>What this does:</strong> When a scene is loaded, the engine uploads meshes,
 * textures, and shaders to the GPU. Each uploaded resource gets an integer ID. This class
 * keeps a record of which IDs belong to a particular scene, so the engine can release them
 * when the scene is unloaded.
 *
 * <p><strong>Why reference counting matters:</strong> Multiple scenes may share the same
 * asset (e.g., a common brick texture used in both a menu scene and a gameplay scene).
 * The GPU systems use reference counting to track how many scenes are using each resource.
 * When a scene is loaded, its assets are "retained" (refcount +1). When unloaded, they are
 * "released" (refcount -1). The GPU memory is only freed when the refcount reaches zero —
 * meaning no scene is using that asset anymore.
 *
 * <p><strong>Lifecycle:</strong>
 * <ol>
 *   <li>Created by {@link SceneManager} during scene instantiation</li>
 *   <li>Populated as entities are resolved (each Mesh, Texture, Shader gets tracked)</li>
 *   <li>All tracked IDs are retained via GPU system {@code retain()} calls</li>
 *   <li>Stored inside a {@link SceneInstance} for the scene's lifetime</li>
 *   <li>All tracked IDs are released via GPU system {@code release()} calls on unload</li>
 * </ol>
 *
 * @see SceneManager
 * @see SceneInstance
 */
class SceneAssets {

    /** Mesh resource IDs uploaded to the GPU for this scene. */
    final List<Integer> meshIds = new ArrayList<>();

    /** Texture resource IDs uploaded to the GPU for this scene. */
    final List<Integer> textureIds = new ArrayList<>();

    /** Compiled shader program IDs uploaded to the GPU for this scene. */
    final List<Integer> shaderProgramIds = new ArrayList<>();
}
