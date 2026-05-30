package org.chapzlock.core.input;

import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.keyboard.KeyPressedEvent;
import org.chapzlock.core.input.keyboard.KeyReleasedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class InputMapTest {

    private EventBus eventBus;
    private InputMap inputMap;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        inputMap = InputMap.builder()
            .action("jump", Key.SPACE)
            .action("move_forward", Key.W, Key.UP)
            .axis("horizontal", Key.A, Key.D)
            .build();
        inputMap.activate(eventBus);
    }

    @AfterEach
    void tearDown() {
        inputMap.deactivate();
        eventBus.clearAllListeners();
    }

    @Test
    void isPressedReturnsFalseByDefault() {
        assertFalse(inputMap.isPressed("jump"));
    }

    @Test
    void isPressedReturnsTrueWhenKeyHeld() {
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_SPACE, 0));
        assertTrue(inputMap.isPressed("jump"));
    }

    @Test
    void isPressedReturnsFalseAfterRelease() {
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_SPACE, 0));
        eventBus.publish(new KeyReleasedEvent(GLFW_KEY_SPACE, 0));
        assertFalse(inputMap.isPressed("jump"));
    }

    @Test
    void actionWithMultipleKeysTriggersOnAny() {
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_UP, 0));
        assertTrue(inputMap.isPressed("move_forward"));

        eventBus.publish(new KeyReleasedEvent(GLFW_KEY_UP, 0));
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_W, 0));
        assertTrue(inputMap.isPressed("move_forward"));
    }

    @Test
    void wasJustPressedTrueOnlyOnPressFrame() {
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_SPACE, 0));
        assertTrue(inputMap.wasJustPressed("jump"));

        inputMap.frameReset();
        assertFalse(inputMap.wasJustPressed("jump"));
        assertTrue(inputMap.isPressed("jump"));
    }

    @Test
    void wasJustReleasedTrueOnlyOnReleaseFrame() {
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_SPACE, 0));
        inputMap.frameReset();

        eventBus.publish(new KeyReleasedEvent(GLFW_KEY_SPACE, 0));
        assertTrue(inputMap.wasJustReleased("jump"));

        inputMap.frameReset();
        assertFalse(inputMap.wasJustReleased("jump"));
    }

    @Test
    void axisReturnsZeroWhenNoKeysPressed() {
        assertEquals(0f, inputMap.getAxis("horizontal"));
    }

    @Test
    void axisReturnsNegativeOneForNegativeKey() {
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_A, 0));
        assertEquals(-1f, inputMap.getAxis("horizontal"));
    }

    @Test
    void axisReturnsPositiveOneForPositiveKey() {
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_D, 0));
        assertEquals(1f, inputMap.getAxis("horizontal"));
    }

    @Test
    void axisReturnsZeroWhenBothKeysPressed() {
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_A, 0));
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_D, 0));
        assertEquals(0f, inputMap.getAxis("horizontal"));
    }

    @Test
    void unknownActionReturnsFalse() {
        assertFalse(inputMap.isPressed("nonexistent"));
        assertFalse(inputMap.wasJustPressed("nonexistent"));
        assertFalse(inputMap.wasJustReleased("nonexistent"));
    }

    @Test
    void unknownAxisReturnsZero() {
        assertEquals(0f, inputMap.getAxis("nonexistent"));
    }

    @Test
    void deactivateStopsRespondingToEvents() {
        inputMap.deactivate();
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_SPACE, 0));
        assertFalse(inputMap.isPressed("jump"));
    }

    @Test
    void reactivateWorksAfterDeactivate() {
        inputMap.deactivate();
        inputMap.activate(eventBus);
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_SPACE, 0));
        assertTrue(inputMap.isPressed("jump"));
    }

    @Test
    void unknownActionLogsWarningOnce() {
        var originalOut = System.out;
        var outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        try {
            inputMap.isPressed("typo_action");
            inputMap.isPressed("typo_action");
            String output = outContent.toString();
            assertEquals(1, output.split("\\[WARN\\]").length - 1,
                "Warning should fire exactly once per unregistered action");
            assertTrue(output.contains("typo_action"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
