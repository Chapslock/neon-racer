package org.chapzlock.core.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class LogEngineTest {

    @BeforeEach
    void setUp() {
        LogEngine.setGlobalLevel(LogLevel.INFO);
        LogEngine.resetClock();
    }

    @AfterEach
    void tearDown() {
        LogEngine.resetOutput();
        LogEngine.setGlobalLevel(LogLevel.INFO);
    }

    @Test
    void logLevel_orderedBySeverity() {
        assertTrue(LogLevel.TRACE.ordinal() < LogLevel.DEBUG.ordinal());
        assertTrue(LogLevel.DEBUG.ordinal() < LogLevel.INFO.ordinal());
        assertTrue(LogLevel.INFO.ordinal() < LogLevel.WARN.ordinal());
        assertTrue(LogLevel.WARN.ordinal() < LogLevel.ERROR.ordinal());
    }

    @Test
    void logLevel_isEnabled_checksThreshold() {
        assertTrue(LogLevel.ERROR.isEnabled(LogLevel.WARN));
        assertTrue(LogLevel.WARN.isEnabled(LogLevel.WARN));
        assertFalse(LogLevel.INFO.isEnabled(LogLevel.WARN));
        assertFalse(LogLevel.DEBUG.isEnabled(LogLevel.WARN));
    }

    @Test
    void logEngine_emitsFormattedOutput() {
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        LogEngine.setOutput(new PrintStream(capture), new PrintStream(new ByteArrayOutputStream()));
        LogEngine.resetClock();

        LogEngine.emit(LogLevel.INFO, "TestClass", "hello world");

        String output = capture.toString();
        assertTrue(output.matches("\\[\\d+\\.\\d{3}s] \\[INFO ] \\[TestClass] hello world\\n"), "Got: " + output);
    }

    @Test
    void logEngine_errorGoesToStderr() {
        ByteArrayOutputStream outCapture = new ByteArrayOutputStream();
        ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
        LogEngine.setOutput(new PrintStream(outCapture), new PrintStream(errCapture));

        LogEngine.emit(LogLevel.ERROR, "Test", "bad thing");

        assertEquals("", outCapture.toString());
        assertTrue(errCapture.toString().contains("bad thing"));
    }

    @Test
    void logEngine_classOverrideTakesPrecedence() {
        LogEngine.setGlobalLevel(LogLevel.INFO);
        LogEngine.setClassLevel("PhysicsSystem", LogLevel.TRACE);

        assertEquals(LogLevel.TRACE, LogEngine.getEffectiveLevel("PhysicsSystem"));
        assertEquals(LogLevel.INFO, LogEngine.getEffectiveLevel("SomeOtherClass"));

        LogEngine.clearClassLevel("PhysicsSystem");
    }
}
