package org.chapzlock.core.ecs;

import java.util.List;

/**
 * Declares a dependency between two component types for validation during scene instantiation.
 *
 * <p><strong>What this does:</strong> Defines a rule that says "if entity has component A,
 * it MUST also have component B." For example: if an entity has a Mesh, it must also have
 * a Transform — otherwise the rendering system wouldn't know where to draw it.
 *
 * <p><strong>How it's used:</strong> The scene instantiation system checks all rules against
 * each entity's resolved component list. If a rule is violated, the entity is skipped with
 * a clear error message telling the developer what to fix.
 *
 * @param component the component type that triggers this rule (e.g., Mesh.class)
 * @param requires the component type that must also be present (e.g., Transform.class)
 * @param message human-readable error message explaining what's wrong and why
 */
public record ComponentRule(
    Class<? extends Component> component,
    Class<? extends Component> requires,
    String message
) {
    /**
     * Factory method for readability: "ComponentRule.requires(Mesh, Transform, msg)".
     */
    public static ComponentRule requires(Class<? extends Component> component,
                                         Class<? extends Component> requires,
                                         String message) {
        return new ComponentRule(component, requires, message);
    }

    /**
     * Returns true if this rule is violated by the given component list.
     *
     * <p>A violation occurs when the trigger component is present but the required
     * companion is absent. If the trigger component isn't in the list at all, the
     * rule doesn't apply (no violation).
     *
     * @param components the resolved component list for an entity
     * @return true if the rule is violated
     */
    public boolean isViolated(List<? extends Component> components) {
        boolean hasComponent = false;
        boolean hasRequired = false;
        for (Component c : components) {
            if (component.isInstance(c)) hasComponent = true;
            if (requires.isInstance(c)) hasRequired = true;
            if (hasComponent && hasRequired) return false;
        }
        return hasComponent && !hasRequired;
    }
}
