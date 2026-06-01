package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.Getter;

/**
 * Stores a human-readable name for an entity, enabling name-based lookup
 * via {@link org.chapzlock.core.ecs.ComponentRegistry#getEntityByName(String)}.
 *
 * <p>Named entities can be found from anywhere with registry access. The name
 * is typically set automatically by the scene builder (from the blueprint name)
 * or by the glTF instantiator (from node names in the 3D file).
 *
 * <p>Names should be unique within a scene. If two entities share the same name,
 * the registry keeps the most recently added one and logs a debug warning.
 *
 * @see org.chapzlock.core.ecs.ComponentRegistry#getEntityByName(String)
 * @see org.chapzlock.core.ecs.ComponentRegistry#hasEntity(String)
 */
@Getter
public class Name implements Component {
    private final String value;

    public Name(String value) {
        this.value = value;
    }
}
