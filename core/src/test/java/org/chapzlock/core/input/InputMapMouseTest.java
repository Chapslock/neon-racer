package org.chapzlock.core.input;

import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.mouse.MouseButtonPressedEvent;
import org.chapzlock.core.input.mouse.MouseButtonReleasedEvent;
import org.chapzlock.core.input.mouse.MouseMovedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class InputMapMouseTest {

    private EventBus eventBus;
    private InputMap inputMap;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        inputMap = InputMap.builder()
            .action("shoot", MouseButton.LEFT)
            .action("aim", MouseButton.RIGHT)
            .action("jump", Key.SPACE)
            .mouseAxis("lookX", "lookY")
            .build();
        inputMap.activate(eventBus);
    }

    @AfterEach
    void tearDown() {
        inputMap.deactivate();
        eventBus.clearAllListeners();
    }

    @Test
    void mouseButtonIsPressedWhenHeld() {
        eventBus.publish(new MouseButtonPressedEvent(GLFW_MOUSE_BUTTON_LEFT, 100, 200));
        assertTrue(inputMap.isPressed("shoot"));
    }

    @Test
    void mouseButtonNotPressedAfterRelease() {
        eventBus.publish(new MouseButtonPressedEvent(GLFW_MOUSE_BUTTON_LEFT, 100, 200));
        eventBus.publish(new MouseButtonReleasedEvent(GLFW_MOUSE_BUTTON_LEFT, 100, 200));
        assertFalse(inputMap.isPressed("shoot"));
    }

    @Test
    void mouseButtonWasJustPressedTrueOnPressFrame() {
        eventBus.publish(new MouseButtonPressedEvent(GLFW_MOUSE_BUTTON_LEFT, 100, 200));
        assertTrue(inputMap.wasJustPressed("shoot"));

        inputMap.frameReset();
        assertFalse(inputMap.wasJustPressed("shoot"));
        // But still held
        assertTrue(inputMap.isPressed("shoot"));
    }

    @Test
    void mouseButtonWasJustReleased() {
        eventBus.publish(new MouseButtonPressedEvent(GLFW_MOUSE_BUTTON_RIGHT, 50, 50));
        inputMap.frameReset();

        eventBus.publish(new MouseButtonReleasedEvent(GLFW_MOUSE_BUTTON_RIGHT, 50, 50));
        assertTrue(inputMap.wasJustReleased("aim"));

        inputMap.frameReset();
        assertFalse(inputMap.wasJustReleased("aim"));
    }

    @Test
    void mouseAxisReturnsZeroByDefault() {
        assertEquals(0f, inputMap.getMouseAxis("lookX"));
        assertEquals(0f, inputMap.getMouseAxis("lookY"));
    }

    @Test
    void mouseAxisReturnsDelta() {
        // First event sets baseline (delta = 0)
        eventBus.publish(new MouseMovedEvent(100, 200));
        assertEquals(0f, inputMap.getMouseAxis("lookX"));
        assertEquals(0f, inputMap.getMouseAxis("lookY"));

        // Second event produces a delta
        eventBus.publish(new MouseMovedEvent(110, 205));
        assertEquals(10f, inputMap.getMouseAxis("lookX"));
        assertEquals(5f, inputMap.getMouseAxis("lookY"));
    }

    @Test
    void mouseAxisResetsOnFrameReset() {
        eventBus.publish(new MouseMovedEvent(100, 200));
        eventBus.publish(new MouseMovedEvent(110, 205));
        assertEquals(10f, inputMap.getMouseAxis("lookX"));

        inputMap.frameReset();
        assertEquals(0f, inputMap.getMouseAxis("lookX"));
        assertEquals(0f, inputMap.getMouseAxis("lookY"));
    }

    @Test
    void mouseAxisAccumulatesMultipleMovesPerFrame() {
        // Baseline
        eventBus.publish(new MouseMovedEvent(100, 200));

        // Two moves in the same frame
        eventBus.publish(new MouseMovedEvent(110, 205));
        eventBus.publish(new MouseMovedEvent(125, 210));

        // Total delta: 125-100 = 25, 210-200 = 10
        assertEquals(25f, inputMap.getMouseAxis("lookX"));
        assertEquals(10f, inputMap.getMouseAxis("lookY"));
    }

    @Test
    void unregisteredMouseAxisReturnsZero() {
        assertEquals(0f, inputMap.getMouseAxis("nonexistent"));
    }

    @Test
    void keyboardActionsStillWorkAlongsideMouseActions() {
        eventBus.publish(new org.chapzlock.core.input.keyboard.KeyPressedEvent(GLFW_KEY_SPACE, 0));
        assertTrue(inputMap.isPressed("jump"));
    }

    @Test
    void deactivateClearsMouseState() {
        eventBus.publish(new MouseButtonPressedEvent(GLFW_MOUSE_BUTTON_LEFT, 100, 200));
        inputMap.deactivate();
        assertFalse(inputMap.isPressed("shoot"));
    }

    @Test
    void mouseAxisBaselineResetsAfterDeactivateAndReactivate() {
        eventBus.publish(new MouseMovedEvent(100, 200));
        eventBus.publish(new MouseMovedEvent(110, 210));

        inputMap.deactivate();
        inputMap.activate(eventBus);

        // After reactivation, first move sets new baseline
        eventBus.publish(new MouseMovedEvent(500, 500));
        assertEquals(0f, inputMap.getMouseAxis("lookX"));
        assertEquals(0f, inputMap.getMouseAxis("lookY"));

        eventBus.publish(new MouseMovedEvent(520, 510));
        assertEquals(20f, inputMap.getMouseAxis("lookX"));
        assertEquals(10f, inputMap.getMouseAxis("lookY"));
    }
}
