package org.chapzlock.core.ecs;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemOrderingTest {

    // --- Stub systems for testing ---

    static class StubSystem implements GameSystem {
        final String name;
        final SystemPhase phase;

        StubSystem(String name, SystemPhase phase) {
            this.name = name;
            this.phase = phase;
        }

        @Override
        public SystemPhase phase() { return phase; }

        @Override
        public String toString() { return name; }
    }

    static class SystemA extends StubSystem {
        SystemA() { super("A", SystemPhase.LOGIC); }
    }

    static class SystemB extends StubSystem {
        SystemB() { super("B", SystemPhase.LOGIC); }
    }

    static class SystemC extends StubSystem {
        SystemC() { super("C", SystemPhase.LOGIC); }
    }

    // Named classes for runAfter constraints
    static class ARunsAfterB extends StubSystem {
        ARunsAfterB() { super("ARunsAfterB", SystemPhase.LOGIC); }
        @Override
        public List<Class<? extends GameSystem>> runAfter() {
            return List.of(SystemB.class);
        }
    }

    static class ARunsAfterBAndC extends StubSystem {
        ARunsAfterBAndC() { super("ARunsAfterBAndC", SystemPhase.LOGIC); }
        @Override
        public List<Class<? extends GameSystem>> runAfter() {
            return List.of(SystemB.class, SystemC.class);
        }
    }

    // Named classes for runBefore constraints
    static class BRunsBeforeA extends StubSystem {
        BRunsBeforeA() { super("BRunsBeforeA", SystemPhase.LOGIC); }
        @Override
        public List<Class<? extends GameSystem>> runBefore() {
            return List.of(SystemA.class);
        }
    }

    static class CRunsBeforeAAndB extends StubSystem {
        CRunsBeforeAAndB() { super("CRunsBeforeAAndB", SystemPhase.LOGIC); }
        @Override
        public List<Class<? extends GameSystem>> runBefore() {
            return List.of(SystemA.class, SystemB.class);
        }
    }

    // Named classes for cycle detection
    static class CycleA extends StubSystem {
        CycleA() { super("CycleA", SystemPhase.LOGIC); }
        @Override
        public List<Class<? extends GameSystem>> runAfter() {
            return List.of(CycleB.class);
        }
    }

    static class CycleB extends StubSystem {
        CycleB() { super("CycleB", SystemPhase.LOGIC); }
        @Override
        public List<Class<? extends GameSystem>> runAfter() {
            return List.of(CycleA.class);
        }
    }

    // Named classes for chain ordering: ChainC -> ChainB -> ChainA
    static class ChainA extends StubSystem {
        ChainA() { super("ChainA", SystemPhase.LOGIC); }
        @Override
        public List<Class<? extends GameSystem>> runAfter() {
            return List.of(ChainB.class);
        }
    }

    static class ChainB extends StubSystem {
        ChainB() { super("ChainB", SystemPhase.LOGIC); }
        @Override
        public List<Class<? extends GameSystem>> runAfter() {
            return List.of(ChainC.class);
        }
    }

    static class ChainC extends StubSystem {
        ChainC() { super("ChainC", SystemPhase.LOGIC); }
    }

    // Named class for stable ordering test
    static class DRunsAfterC extends StubSystem {
        DRunsAfterC() { super("DRunsAfterC", SystemPhase.LOGIC); }
        @Override
        public List<Class<? extends GameSystem>> runAfter() {
            return List.of(SystemC.class);
        }
    }

    // Named class referencing non-existent system
    static class RunsAfterMissing extends StubSystem {
        RunsAfterMissing() { super("RunsAfterMissing", SystemPhase.LOGIC); }
        @Override
        public List<Class<? extends GameSystem>> runAfter() {
            return List.of(SystemC.class); // C won't be in the list
        }
    }

    // --- No constraints ---

    @Test
    void noConstraints_preservesInsertionOrder() {
        var a = new SystemA();
        var b = new SystemB();
        var c = new SystemC();

        List<GameSystem> result = SystemOrdering.sort(List.of(a, b, c));

        assertEquals(List.of(a, b, c), result);
    }

    @Test
    void noConstraints_sortsByPhase() {
        var render = new StubSystem("render", SystemPhase.RENDER);
        var input = new StubSystem("input", SystemPhase.INPUT);
        var logic = new StubSystem("logic", SystemPhase.LOGIC);
        var physics = new StubSystem("physics", SystemPhase.PHYSICS);

        List<GameSystem> result = SystemOrdering.sort(List.of(render, input, logic, physics));

        assertEquals(List.of(input, logic, physics, render), result);
    }

    // --- runAfter constraints ---

    @Test
    void runAfter_placesSystemAfterDependency() {
        var a = new ARunsAfterB();
        var b = new SystemB();

        // A says "run after B", so even if A comes first in input, B must come first in output
        List<GameSystem> result = SystemOrdering.sort(List.of(a, b));

        assertEquals(0, result.indexOf(b));
        assertEquals(1, result.indexOf(a));
    }

    @Test
    void runAfter_multipleConstraints() {
        var a = new ARunsAfterBAndC();
        var b = new SystemB();
        var c = new SystemC();

        List<GameSystem> result = SystemOrdering.sort(List.of(a, b, c));

        // A must come after both B and C
        assertTrue(result.indexOf(a) > result.indexOf(b));
        assertTrue(result.indexOf(a) > result.indexOf(c));
    }

    // --- runBefore constraints ---

    @Test
    void runBefore_placesSystemBeforeDependency() {
        var a = new SystemA();
        var b = new BRunsBeforeA();

        // B says "run before A", so B must come before A regardless of input order
        List<GameSystem> result = SystemOrdering.sort(List.of(a, b));

        assertTrue(result.indexOf(b) < result.indexOf(a));
    }

    @Test
    void runBefore_multipleConstraints() {
        var c = new CRunsBeforeAAndB();
        var a = new SystemA();
        var b = new SystemB();

        List<GameSystem> result = SystemOrdering.sort(List.of(a, b, c));

        // C must come before both A and B
        assertTrue(result.indexOf(c) < result.indexOf(a));
        assertTrue(result.indexOf(c) < result.indexOf(b));
    }

    // --- Cycle detection ---

    @Test
    void cycle_throwsIllegalStateException() {
        var a = new CycleA();
        var b = new CycleB();

        // CycleA runAfter CycleB, CycleB runAfter CycleA -> cycle
        assertThrows(IllegalStateException.class, () -> SystemOrdering.sort(List.of(a, b)));
    }

    // --- Cross-phase constraints (should be ignored gracefully) ---

    @Test
    void crossPhaseRunAfter_isIgnored() {
        var inputSys = new StubSystem("input", SystemPhase.INPUT);
        var logicSys = new StubSystem("logic", SystemPhase.LOGIC) {
            @Override
            public List<Class<? extends GameSystem>> runAfter() {
                // References a system in INPUT phase - should be ignored since different phase
                return List.of(inputSys.getClass());
            }
        };

        // Should not throw; cross-phase constraints are silently ignored
        List<GameSystem> result = SystemOrdering.sort(List.of(inputSys, logicSys));

        // Phase ordering still applies: INPUT before LOGIC
        assertEquals(inputSys, result.get(0));
        assertEquals(logicSys, result.get(1));
    }

    @Test
    void crossPhaseRunBefore_isIgnored() {
        var logicSys = new StubSystem("logic", SystemPhase.LOGIC) {
            @Override
            public List<Class<? extends GameSystem>> runBefore() {
                // References a system in RENDER phase - should be ignored
                return List.of(StubSystem.class);
            }
        };
        var renderSys = new StubSystem("render", SystemPhase.RENDER);

        // Should not throw
        List<GameSystem> result = SystemOrdering.sort(List.of(renderSys, logicSys));

        // Phase ordering: LOGIC before RENDER
        assertEquals(logicSys, result.get(0));
        assertEquals(renderSys, result.get(1));
    }

    // --- Combined constraints within a phase ---

    @Test
    void combinedConstraints_chainOrdering() {
        // Chain: C -> B -> A (C runs first, then B, then A)
        var a = new ChainA(); // runs after ChainB
        var b = new ChainB(); // runs after ChainC
        var c = new ChainC(); // no constraints

        List<GameSystem> result = SystemOrdering.sort(List.of(a, b, c));

        assertEquals(0, result.indexOf(c));
        assertEquals(1, result.indexOf(b));
        assertEquals(2, result.indexOf(a));
    }

    // --- Stable ordering (tiebreaker) ---

    @Test
    void stableOrder_unconstrainedSystemsMaintainOriginalPosition() {
        var a = new SystemA();
        var b = new SystemB();
        var c = new SystemC();
        var d = new DRunsAfterC();

        // Input: A, B, C, D. Only constraint: D after C
        // Expected: A, B, C, D (A and B unconstrained, maintain order; D goes after C)
        List<GameSystem> result = SystemOrdering.sort(List.of(a, b, c, d));

        assertTrue(result.indexOf(a) < result.indexOf(b)); // stable: A before B
        assertTrue(result.indexOf(c) < result.indexOf(d)); // D after C
    }

    // --- Empty list ---

    @Test
    void emptyList_returnsEmpty() {
        List<GameSystem> result = SystemOrdering.sort(List.of());
        assertTrue(result.isEmpty());
    }

    // --- Single system ---

    @Test
    void singleSystem_returnsSame() {
        var a = new SystemA();
        List<GameSystem> result = SystemOrdering.sort(List.of(a));
        assertEquals(List.of(a), result);
    }

    // --- runAfter referencing non-existent system (not in the list) ---

    @Test
    void runAfter_nonExistentSystem_isIgnored() {
        var a = new RunsAfterMissing(); // References SystemC which is not in the list
        var b = new SystemB();

        // Should not throw; the missing dependency is silently ignored
        List<GameSystem> result = SystemOrdering.sort(List.of(a, b));

        assertEquals(2, result.size());
    }
}
