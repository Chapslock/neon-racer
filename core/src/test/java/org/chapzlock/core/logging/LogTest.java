package org.chapzlock.core.logging;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogTest {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        Log.setDebugEnabled(true);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        Log.setDebugEnabled(false);
    }

    @Test
    void debugLogsWhenEnabled() {
        Log.debug("test message");
        assertTrue(outContent.toString().contains("[DEBUG] test message"));
    }

    @Test
    void debugSilentWhenDisabled() {
        Log.setDebugEnabled(false);
        Log.debug("hidden");
        assertFalse(outContent.toString().contains("hidden"));
    }

    @Test
    void errorWithThrowableIncludesStackTrace() {
        RuntimeException ex = new RuntimeException("boom");
        Log.error("something failed", ex);
        String output = errContent.toString();
        assertTrue(output.contains("[ERROR] something failed"));
        assertTrue(output.contains("boom"));
    }

    @Test
    void infoFormatsCorrectly() {
        Log.info("hello");
        assertTrue(outContent.toString().contains("[INFO] hello"));
    }

    @Test
    void warnFormatsCorrectly() {
        Log.warn("careful");
        assertTrue(outContent.toString().contains("[WARN] careful"));
    }

    @Test
    void errorFormatsCorrectly() {
        Log.error("bad");
        assertTrue(errContent.toString().contains("[ERROR] bad"));
    }
}
