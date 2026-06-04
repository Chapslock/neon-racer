package org.chapzlock.core.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class LogTest {

    private ByteArrayOutputStream outCapture;
    private ByteArrayOutputStream errCapture;

    @BeforeEach
    void setUp() {
        outCapture = new ByteArrayOutputStream();
        errCapture = new ByteArrayOutputStream();
        LogEngine.setOutput(new PrintStream(outCapture), new PrintStream(errCapture));
        LogEngine.setGlobalLevel(LogLevel.TRACE);
        LogEngine.resetClock();
    }

    @AfterEach
    void tearDown() {
        LogEngine.resetOutput();
        LogEngine.setGlobalLevel(LogLevel.INFO);
    }

    @Test
    void create_usesSimpleClassName() {
        Log log = Log.create(LogTest.class);
        log.info("test message");
        assertTrue(outCapture.toString().contains("[LogTest]"));
    }

    @Test
    void debug_whenLevelIsInfo_doesNotOutput() {
        LogEngine.setGlobalLevel(LogLevel.INFO);
        Log log = Log.create(LogTest.class);
        log.debug("should not appear");
        assertEquals("", outCapture.toString());
    }

    @Test
    void debug_whenLevelIsDebug_outputs() {
        LogEngine.setGlobalLevel(LogLevel.DEBUG);
        Log log = Log.create(LogTest.class);
        log.debug("should appear");
        assertTrue(outCapture.toString().contains("should appear"));
    }

    @Test
    void trace_whenLevelIsDebug_doesNotOutput() {
        LogEngine.setGlobalLevel(LogLevel.DEBUG);
        Log log = Log.create(LogTest.class);
        log.trace("too verbose");
        assertEquals("", outCapture.toString());
    }

    @Test
    void error_goesToStderr() {
        Log log = Log.create(LogTest.class);
        log.error("broken");
        assertEquals("", outCapture.toString());
        assertTrue(errCapture.toString().contains("broken"));
    }

    @Test
    void error_withThrowable_printsStackTrace() {
        Log log = Log.create(LogTest.class);
        log.error("failed", new RuntimeException("cause"));
        String errOutput = errCapture.toString();
        assertTrue(errOutput.contains("failed"));
        assertTrue(errOutput.contains("RuntimeException"));
        assertTrue(errOutput.contains("cause"));
    }

    @Test
    void formatString_interpolatesArguments() {
        Log log = Log.create(LogTest.class);
        log.info("entity=%d name=%s", 42, "player");
        assertTrue(outCapture.toString().contains("entity=42 name=player"));
    }

    @Test
    void isDebugEnabled_reflectsLevel() {
        LogEngine.setGlobalLevel(LogLevel.INFO);
        Log log = Log.create(LogTest.class);
        assertFalse(log.isDebugEnabled());

        LogEngine.setGlobalLevel(LogLevel.DEBUG);
        assertTrue(log.isDebugEnabled());
    }

    @Test
    void isTraceEnabled_reflectsLevel() {
        LogEngine.setGlobalLevel(LogLevel.DEBUG);
        Log log = Log.create(LogTest.class);
        assertFalse(log.isTraceEnabled());

        LogEngine.setGlobalLevel(LogLevel.TRACE);
        assertTrue(log.isTraceEnabled());
    }

    @Test
    void classOverride_overridesGlobalLevel() {
        LogEngine.setGlobalLevel(LogLevel.WARN);
        LogEngine.setClassLevel("LogTest", LogLevel.DEBUG);

        Log log = Log.create(LogTest.class);
        log.debug("visible due to override");
        assertTrue(outCapture.toString().contains("visible due to override"));

        LogEngine.clearClassLevel("LogTest");
    }

    @Test
    void outputFormat_containsTimestampLevelAndSource() {
        Log log = Log.create(LogTest.class);
        log.warn("test");
        String output = outCapture.toString();
        assertTrue(output.matches("\\[\\d+\\.\\d{3}s] \\[WARN ] \\[LogTest] test\\n"), "Got: " + output);
    }
}
