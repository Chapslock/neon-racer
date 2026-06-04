package org.chapzlock.core.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class LogTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream outCapture;
    private ByteArrayOutputStream errCapture;

    @BeforeEach
    void setUp() {
        outCapture = new ByteArrayOutputStream();
        errCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outCapture));
        System.setErr(new PrintStream(errCapture));
        Log.setDebugEnabled(true);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void debug_withoutTag_formatsWithoutBrackets() {
        Log.debug("hello world");
        assertEquals("[DEBUG] hello world\n", outCapture.toString());
    }

    @Test
    void debug_withTag_includesTagInBrackets() {
        Log.debug("ShaderRegistry", "compiled program 5");
        assertEquals("[DEBUG] [ShaderRegistry] compiled program 5\n", outCapture.toString());
    }

    @Test
    void info_withTag_includesTagInBrackets() {
        Log.info("Engine", "startup complete");
        assertEquals("[INFO] [Engine] startup complete\n", outCapture.toString());
    }

    @Test
    void warn_withTag_includesTagInBrackets() {
        Log.warn("RenderContext", "no camera found");
        assertEquals("[WARN] [RenderContext] no camera found\n", outCapture.toString());
    }

    @Test
    void error_withTag_includesTagInBrackets() {
        Log.error("SceneManager", "failed to load");
        assertEquals("[ERROR] [SceneManager] failed to load\n", errCapture.toString());
    }

    @Test
    void debug_whenDisabled_doesNotOutput() {
        Log.setDebugEnabled(false);
        Log.debug("ShaderRegistry", "should not appear");
        assertEquals("", outCapture.toString());
    }
}
