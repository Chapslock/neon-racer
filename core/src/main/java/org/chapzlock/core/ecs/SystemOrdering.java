package org.chapzlock.core.ecs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Sorts game systems by phase and resolves within-phase ordering constraints.
 *
 * <p>Systems are first grouped by their {@link SystemPhase}. Within each phase group,
 * a topological sort (Kahn's algorithm) is applied to honour {@link GameSystem#runBefore()}
 * and {@link GameSystem#runAfter()} constraints. Systems with no constraints maintain their
 * original input order (stable sort).
 *
 * <p>Cross-phase constraints are silently ignored since phase ordering already handles
 * the relative execution order between phases.
 *
 * @see GameSystem#runBefore()
 * @see GameSystem#runAfter()
 */
public final class SystemOrdering {

    private SystemOrdering() {}

    /**
     * Sorts systems by phase, then applies topological ordering within each phase.
     *
     * @param systems the systems to sort (not modified)
     * @return a new list with systems in correct execution order
     * @throws IllegalStateException if a dependency cycle is detected within a phase
     */
    public static List<GameSystem> sort(List<GameSystem> systems) {
        if (systems.isEmpty()) {
            return List.of();
        }

        // Group systems by phase, preserving insertion order within each group
        Map<SystemPhase, List<GameSystem>> byPhase = new HashMap<>();
        for (GameSystem system : systems) {
            byPhase.computeIfAbsent(system.phase(), k -> new ArrayList<>()).add(system);
        }

        // Sort phases by ordinal, then topologically sort within each phase
        List<GameSystem> result = new ArrayList<>(systems.size());
        for (SystemPhase phase : SystemPhase.values()) {
            List<GameSystem> group = byPhase.get(phase);
            if (group != null) {
                result.addAll(topologicalSort(group));
            }
        }

        return List.copyOf(result);
    }

    /**
     * Performs Kahn's algorithm on systems within a single phase.
     *
     * <p>Edges are derived from runBefore/runAfter constraints. Only constraints referencing
     * systems actually present in the group (and same phase) are considered. Missing or
     * cross-phase references are silently ignored.
     *
     * <p>Uses original input index as tiebreaker for stable ordering among unconstrained systems.
     */
    private static List<GameSystem> topologicalSort(List<GameSystem> systems) {
        int n = systems.size();
        if (n <= 1) {
            return new ArrayList<>(systems);
        }

        // Map system class to its index in the group (for constraint resolution)
        // A system may be identified by its concrete class or any superclass that's a GameSystem
        Map<Class<?>, Integer> classToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            classToIndex.put(systems.get(i).getClass(), i);
        }

        // Build adjacency list and in-degree array
        // Edge from A -> B means "A must run before B"
        List<List<Integer>> adj = new ArrayList<>(n);
        int[] inDegree = new int[n];
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }

        for (int i = 0; i < n; i++) {
            GameSystem system = systems.get(i);

            // runAfter: this system runs after the referenced systems
            // So referenced -> this (edge from referenced to this)
            for (Class<? extends GameSystem> depClass : system.runAfter()) {
                Integer depIndex = findIndex(classToIndex, depClass, systems);
                if (depIndex != null && depIndex != i) {
                    adj.get(depIndex).add(i);
                    inDegree[i]++;
                }
            }

            // runBefore: this system runs before the referenced systems
            // So this -> referenced (edge from this to referenced)
            for (Class<? extends GameSystem> targetClass : system.runBefore()) {
                Integer targetIndex = findIndex(classToIndex, targetClass, systems);
                if (targetIndex != null && targetIndex != i) {
                    adj.get(i).add(targetIndex);
                    inDegree[targetIndex]++;
                }
            }
        }

        // Kahn's algorithm with priority queue for stable ordering (tiebreak by original index)
        Queue<Integer> queue = new ArrayDeque<>();
        // Collect all zero-in-degree nodes, sorted by original index for stability
        List<Integer> zeroDegree = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                zeroDegree.add(i);
            }
        }
        zeroDegree.sort(Comparator.naturalOrder());
        queue.addAll(zeroDegree);

        List<GameSystem> result = new ArrayList<>(n);
        while (!queue.isEmpty()) {
            int current = queue.poll();
            result.add(systems.get(current));

            // Collect newly freed neighbors and sort by original index for stability
            List<Integer> freed = new ArrayList<>();
            for (int neighbor : adj.get(current)) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    freed.add(neighbor);
                }
            }
            freed.sort(Comparator.naturalOrder());
            queue.addAll(freed);
        }

        if (result.size() != n) {
            // Cycle detected - find the systems involved for a useful error message
            List<String> cycleMembers = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (inDegree[i] > 0) {
                    cycleMembers.add(systems.get(i).getClass().getSimpleName());
                }
            }
            throw new IllegalStateException(
                "Dependency cycle detected among systems in phase " +
                systems.get(0).phase() + ": " + cycleMembers);
        }

        return result;
    }

    /**
     * Finds the index of a system matching the given class. Checks exact class match first,
     * then falls back to instanceof checks for systems that extend the target class.
     */
    private static Integer findIndex(Map<Class<?>, Integer> classToIndex,
                                     Class<? extends GameSystem> targetClass,
                                     List<GameSystem> systems) {
        // Exact match first (most common case)
        Integer index = classToIndex.get(targetClass);
        if (index != null) {
            return index;
        }

        // Fallback: check if any system is an instance of the target class
        for (int i = 0; i < systems.size(); i++) {
            if (targetClass.isAssignableFrom(systems.get(i).getClass())) {
                return i;
            }
        }

        return null;
    }
}
