package org.chapzlock.core.ecs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SystemSchedulerTest {

    private SystemScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SystemScheduler();
    }

    @Test
    void getActiveSystems_emptyByDefault() {
        List<GameSystem> systems = scheduler.getActiveSystems();
        assertTrue(systems.isEmpty());
    }

    @Test
    void addPermanent_includesInActiveSystems() {
        GameSystem system = new StubLogicSystem();
        scheduler.addPermanent(system);

        List<GameSystem> active = scheduler.getActiveSystems();
        assertEquals(1, active.size());
        assertSame(system, active.get(0));
    }

    @Test
    void addSceneSystems_includesInActiveSystems() {
        GameSystem scene1 = new StubLogicSystem();
        scheduler.addSceneSystems(List.of(scene1));

        List<GameSystem> active = scheduler.getActiveSystems();
        assertEquals(1, active.size());
        assertSame(scene1, active.get(0));
    }

    @Test
    void removeSceneSystems_removesFromActiveSystems() {
        GameSystem scene1 = new StubLogicSystem();
        scheduler.addSceneSystems(List.of(scene1));
        scheduler.removeSceneSystems(List.of(scene1));

        List<GameSystem> active = scheduler.getActiveSystems();
        assertTrue(active.isEmpty());
    }

    @Test
    void activeSystems_sortedByPhase() {
        GameSystem render = new StubRenderSystem();
        GameSystem logic = new StubLogicSystem();
        scheduler.addPermanent(render);
        scheduler.addSceneSystems(List.of(logic));

        List<GameSystem> active = scheduler.getActiveSystems();
        assertEquals(2, active.size());
        assertEquals(SystemPhase.LOGIC, active.get(0).phase());
        assertEquals(SystemPhase.RENDER, active.get(1).phase());
    }

    @Test
    void activeSystems_cacheInvalidatesOnChange() {
        GameSystem logic = new StubLogicSystem();
        scheduler.addPermanent(logic);

        List<GameSystem> first = scheduler.getActiveSystems();
        GameSystem render = new StubRenderSystem();
        scheduler.addSceneSystems(List.of(render));
        List<GameSystem> second = scheduler.getActiveSystems();

        assertEquals(1, first.size());
        assertEquals(2, second.size());
    }

    private static class StubLogicSystem implements GameSystem {}

    private static class StubRenderSystem implements GameSystem {
        @Override
        public SystemPhase phase() { return SystemPhase.RENDER; }
    }
}
