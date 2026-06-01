package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import java.util.Arrays;

/**
 * Defines parent-child relationships between entities to form a scene graph.
 * <p>
 * A scene graph is a tree structure where entities can have one parent and multiple children.
 * Child transforms become relative to their parent's transform, so moving the parent automatically
 * moves all its children with it. This is commonly used for:
 * <ul>
 *   <li>Attaching objects to moving entities (e.g., a weapon held by a character)</li>
 *   <li>Creating articulated structures (e.g., a robot arm with multiple joints)</li>
 *   <li>Organizing complex 3D models loaded from files like glTF</li>
 * </ul>
 * <p>
 * When loading 3D models from formats like glTF, nodes with children automatically create
 * these hierarchical relationships. The child's Transform component becomes relative to its
 * parent rather than being in world space.
 * <p>
 * Each entity can have at most one parent, but may have any number of children. Root entities
 * have no parent (parent ID = {@link #NO_PARENT}) and their transforms are in world space.
 */
public class Hierarchy implements Component {

    /**
     * Constant indicating an entity has no parent (is a root entity).
     */
    public static final int NO_PARENT = -1;

    private static final int INITIAL_CAPACITY = 4;

    private int parent;
    private int[] children;
    private int childCount;

    /**
     * Private constructor. Use {@link #root()} or {@link #childOf(int)} to create instances.
     *
     * @param parent the parent entity ID, or {@link #NO_PARENT} for root entities
     */
    private Hierarchy(int parent) {
        this.parent = parent;
        this.children = new int[INITIAL_CAPACITY];
        this.childCount = 0;
    }

    /**
     * Creates a root hierarchy node with no parent.
     * <p>
     * Root entities exist at the top level of the scene graph. Their Transform components
     * represent positions in world space rather than being relative to a parent.
     *
     * @return a new Hierarchy component with no parent
     */
    public static Hierarchy root() {
        return new Hierarchy(NO_PARENT);
    }

    /**
     * Creates a child hierarchy node with the specified parent entity.
     * <p>
     * The child entity's Transform will be interpreted relative to the parent's Transform.
     * For example, if the parent is at position (10, 0, 0) and the child's Transform has
     * position (5, 0, 0), the child's actual world position will be (15, 0, 0).
     *
     * @param parentEntityId the entity ID of the parent
     * @return a new Hierarchy component with the specified parent
     */
    public static Hierarchy childOf(int parentEntityId) {
        return new Hierarchy(parentEntityId);
    }

    /**
     * Returns the parent entity ID, or {@link #NO_PARENT} if this is a root entity.
     *
     * @return the parent entity ID
     */
    public int getParent() {
        return parent;
    }

    /**
     * Sets the parent entity ID.
     * <p>
     * This allows reparenting an entity in the scene graph. The entity's Transform will
     * now be interpreted relative to the new parent.
     *
     * @param parent the new parent entity ID, or {@link #NO_PARENT} to make this a root
     */
    public void setParent(int parent) {
        this.parent = parent;
    }

    /**
     * Returns whether this is a root entity (has no parent).
     *
     * @return true if this entity has no parent, false otherwise
     */
    public boolean isRoot() {
        return parent == NO_PARENT;
    }

    /**
     * Returns a copy of the children array.
     * <p>
     * The returned array contains the entity IDs of all children. Modifications to the
     * returned array do not affect this component. Use {@link #addChild(int)} and
     * {@link #removeChild(int)} to modify the children list.
     *
     * @return a copy of the children entity IDs
     */
    public int[] getChildren() {
        return Arrays.copyOf(children, childCount);
    }

    /**
     * Returns the number of children this entity has.
     *
     * @return the child count
     */
    public int getChildCount() {
        return childCount;
    }

    /**
     * Adds a child entity to this hierarchy node.
     * <p>
     * The child entity should also have its own Hierarchy component with this entity
     * set as the parent. The scene graph system is responsible for maintaining this
     * bidirectional relationship.
     * <p>
     * If the internal children array is full, it will be automatically grown by doubling
     * its capacity.
     *
     * @param childEntityId the entity ID to add as a child
     */
    public void addChild(int childEntityId) {
        if (childCount == children.length) {
            children = Arrays.copyOf(children, children.length * 2);
        }
        children[childCount++] = childEntityId;
    }

    /**
     * Removes a child entity from this hierarchy node.
     * <p>
     * If the child entity is not present in the children list, this method does nothing.
     * The removal is performed by shifting all subsequent children down to fill the gap.
     *
     * @param childEntityId the entity ID to remove from the children list
     */
    public void removeChild(int childEntityId) {
        for (int i = 0; i < childCount; i++) {
            if (children[i] == childEntityId) {
                // Shift remaining children down
                System.arraycopy(children, i + 1, children, i, childCount - i - 1);
                childCount--;
                return;
            }
        }
    }
}
