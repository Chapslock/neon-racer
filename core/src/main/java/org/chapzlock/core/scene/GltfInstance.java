package org.chapzlock.core.scene;

import java.util.Map;

/**
 * Handle to a loaded glTF scene or prefab instance.
 *
 * <p>When a glTF file is instantiated, each named node becomes an entity in the ECS.
 * This class provides name-based lookup so gameplay code can find those entities
 * without hard-coding entity IDs.
 *
 * <p><strong>What is glTF?</strong><br>
 * glTF (GL Transmission Format) is an open standard for 3D models and scenes.
 * Tools like Blender export scenes to {@code .glb} or {@code .gltf} files that
 * contain meshes, materials, transforms, and a node hierarchy. Each node in the
 * hierarchy has an optional name (set in the DCC tool) that becomes the lookup key here.
 *
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * GltfInstance track = sceneManager.loadScene("track.glb");
 * int player = track.entity("Player");      // Look up by Blender node name
 * int camera = track.entity("MainCamera");  // Another named node
 *
 * // Safe check before lookup:
 * if (track.hasEntity("Checkpoint_01")) {
 *     int checkpoint = track.entity("Checkpoint_01");
 * }
 * }</pre>
 *
 * <p><strong>How names work:</strong><br>
 * Names come from the node names in the glTF file (set in Blender's outliner or
 * other DCC tools). Only nodes that have a name in the glTF file are queryable
 * through this class — unnamed nodes still become entities but cannot be looked
 * up by name. Names are case-sensitive and must match exactly.
 *
 * <p><strong>Immutability:</strong><br>
 * This class is immutable once constructed. The internal name-to-entity map is
 * defensively copied, so modifications to the original map will not affect this instance.
 *
 * @see SceneManager
 */
public class GltfInstance {

    /** The entity ID of the root node (or first root if the scene has multiple roots). */
    private final int rootEntityId;

    /**
     * Mapping from glTF node names to their corresponding entity IDs.
     * This map is unmodifiable (created via {@link Map#copyOf}).
     */
    private final Map<String, Integer> nameToEntityId;

    /**
     * Creates a new glTF instance handle.
     *
     * <p>This constructor is typically called by the scene instantiation system after
     * it has created all entities from the glTF node hierarchy. Gameplay code receives
     * an already-constructed instance and uses the lookup methods.
     *
     * @param rootEntityId   the entity ID of the first root node in the glTF scene
     * @param nameToEntityId mapping of glTF node names to their entity IDs;
     *                       a defensive copy is made, so the caller can safely mutate
     *                       the original map afterward
     */
    public GltfInstance(int rootEntityId, Map<String, Integer> nameToEntityId) {
        this.rootEntityId = rootEntityId;
        this.nameToEntityId = Map.copyOf(nameToEntityId);
    }

    /**
     * Returns the entity ID of the root node.
     *
     * <p>If the glTF file has multiple root nodes, this returns the first one.
     * The root entity is useful as a parent for scene-level operations like
     * moving the entire loaded scene.
     *
     * @return the root entity ID
     */
    public int rootEntity() {
        return rootEntityId;
    }

    /**
     * Looks up an entity by its glTF node name.
     *
     * <p>The name must match exactly (case-sensitive) the node name from the glTF file.
     * For Blender exports, this is the name shown in the Outliner panel.
     *
     * <p>If you are unsure whether a name exists, use {@link #hasEntity(String)} first
     * to avoid the exception.
     *
     * @param name the node name from the glTF file (e.g., "Player", "MainCamera")
     * @return the entity ID corresponding to that named node
     * @throws IllegalArgumentException if no entity has that name in this instance
     */
    public int entity(String name) {
        Integer id = nameToEntityId.get(name);
        if (id == null) {
            throw new IllegalArgumentException(
                "No entity named \"" + name + "\" in this glTF instance. " +
                "Available names: " + nameToEntityId.keySet());
        }
        return id;
    }

    /**
     * Checks whether an entity with the given name exists in this instance.
     *
     * <p>Use this for optional lookups where the node may or may not be present
     * in the glTF file (e.g., optional checkpoints, variant meshes).
     *
     * @param name the node name to check (case-sensitive)
     * @return {@code true} if an entity with this name exists, {@code false} otherwise
     */
    public boolean hasEntity(String name) {
        return nameToEntityId.containsKey(name);
    }

    /**
     * Returns all name-to-entity mappings as an unmodifiable map.
     *
     * <p>This is useful for debugging or iterating over all named entities
     * in the instance (e.g., to find all entities matching a naming pattern).
     *
     * @return an unmodifiable map of node names to entity IDs
     */
    public Map<String, Integer> allEntities() {
        return nameToEntityId;
    }
}
