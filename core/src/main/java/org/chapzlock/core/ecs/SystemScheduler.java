package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the unified tick list for the engine's main loop.
 *
 * <p>Engine registers permanent systems (always active) at startup.
 * SceneManager adds/removes scene systems on load/unload.
 * The scheduler produces a sorted execution order via {@link SystemOrdering}.
 */
public class SystemScheduler {

    private final List<GameSystem> permanentSystems = new ArrayList<>();
    private final List<GameSystem> sceneSystems = new ArrayList<>();
    private List<GameSystem> cachedOrder;
    private boolean dirty = true;

    /**
     * Registers a system that lives for the engine's entire lifetime.
     *
     * @param system the permanent system to register
     */
    public void addPermanent(GameSystem system) {
        permanentSystems.add(system);
        dirty = true;
    }

    /**
     * Adds scene-specific systems (called on scene load).
     *
     * @param systems the scene systems to add
     */
    public void addSceneSystems(List<GameSystem> systems) {
        sceneSystems.addAll(systems);
        dirty = true;
    }

    /**
     * Removes scene-specific systems (called on scene unload).
     *
     * @param systems the scene systems to remove
     */
    public void removeSceneSystems(List<GameSystem> systems) {
        sceneSystems.removeAll(systems);
        dirty = true;
    }

    /**
     * Returns all active systems sorted by phase and ordering constraints.
     *
     * <p>The result is cached and only rebuilt when systems are added/removed.
     *
     * @return sorted list of all active systems
     */
    public List<GameSystem> getActiveSystems() {
        if (dirty || cachedOrder == null) {
            List<GameSystem> all = new ArrayList<>(permanentSystems);
            all.addAll(sceneSystems);
            cachedOrder = SystemOrdering.sort(all);
            dirty = false;
        }
        return cachedOrder;
    }
}
