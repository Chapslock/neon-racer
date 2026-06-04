package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemPhaseOrderingTest {

    static class RecordingSystem implements GameSystem {
        final String name;
        final SystemPhase phase;
        final List<String> log;

        RecordingSystem(String name, SystemPhase phase, List<String> log) {
            this.name = name;
            this.phase = phase;
            this.log = log;
        }

        @Override
        public SystemPhase phase() { return phase; }

        @Override
        public void onUpdate(float deltaTime) { log.add("update:" + name); }

        @Override
        public void onRender(float deltaTime) { log.add("render:" + name); }
    }

    @Test
    void systemsAreSortedByPhaseOrdinal() {
        List<String> log = new ArrayList<>();
        List<GameSystem> systems = List.of(
            new RecordingSystem("render", SystemPhase.RENDER, log),
            new RecordingSystem("physics", SystemPhase.PHYSICS, log),
            new RecordingSystem("input", SystemPhase.INPUT, log),
            new RecordingSystem("logic", SystemPhase.LOGIC, log)
        );

        List<GameSystem> sorted = systems.stream()
            .sorted((a, b) -> Integer.compare(a.phase().ordinal(), b.phase().ordinal()))
            .toList();

        sorted.forEach(s -> s.onUpdate(0.016f));

        assertEquals(List.of("update:input", "update:logic", "update:physics", "update:render"), log);
    }

    @Test
    void systemsWithSamePhasePreserveInsertionOrder() {
        List<String> log = new ArrayList<>();
        List<GameSystem> systems = List.of(
            new RecordingSystem("logicB", SystemPhase.LOGIC, log),
            new RecordingSystem("logicA", SystemPhase.LOGIC, log),
            new RecordingSystem("logicC", SystemPhase.LOGIC, log)
        );

        List<GameSystem> sorted = systems.stream()
            .sorted((a, b) -> Integer.compare(a.phase().ordinal(), b.phase().ordinal()))
            .toList();

        sorted.forEach(s -> s.onUpdate(0.016f));

        assertEquals(List.of("update:logicB", "update:logicA", "update:logicC"), log);
    }

    @Test
    void defaultPhaseIsLogic() {
        GameSystem defaultSystem = new GameSystem() {};
        assertEquals(SystemPhase.LOGIC, defaultSystem.phase());
    }
}
