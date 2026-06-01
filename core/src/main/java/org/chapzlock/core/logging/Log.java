package org.chapzlock.core.logging;

import lombok.experimental.UtilityClass;

/**
 * Simple logging utility for the engine.
 *
 * <p>Provides leveled logging (debug, info, warn, error) with optional source tags.
 * Tags help identify which system produced a log message when multiple systems are
 * logging simultaneously.
 *
 * <p><strong>Levels:</strong>
 * <ul>
 *   <li>{@code debug} — Verbose output for development. Only prints when debug is enabled
 *       via {@code -Dengine.debug=true} or {@link #setDebugEnabled(boolean)}.</li>
 *   <li>{@code info} — Normal operational messages (system startup, scene transitions).</li>
 *   <li>{@code warn} — Something unexpected that doesn't prevent operation (missing optional
 *       resource, deprecated usage).</li>
 *   <li>{@code error} — Something that prevents correct operation (failed to load required
 *       asset, shader compilation failure).</li>
 * </ul>
 *
 * <p><strong>Output format:</strong>
 * <pre>
 * [LEVEL] message                    — without tag
 * [LEVEL] [Tag] message              — with tag
 * </pre>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 * Log.debug("Loading scene...");                    // tagless
 * Log.debug("ShaderSystem", "Compiled programId=5"); // tagged
 * Log.warn("RenderContext", "No camera found");
 * }</pre>
 */
@UtilityClass
public class Log {

    private static boolean debugEnabled = Boolean.getBoolean("engine.debug");

    /** Enables or disables debug logging at runtime. */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    /** Returns true if debug logging is enabled. */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /** Logs a debug message (only if debug is enabled). */
    public static void debug(String s) {
        if (debugEnabled) {
            System.out.println("[DEBUG] " + s);
        }
    }

    /** Logs a tagged debug message (only if debug is enabled). */
    public static void debug(String tag, String s) {
        if (debugEnabled) {
            System.out.println("[DEBUG] [" + tag + "] " + s);
        }
    }

    /** Logs an info message. */
    public static void info(String s) {
        System.out.println("[INFO] " + s);
    }

    /** Logs a tagged info message. */
    public static void info(String tag, String s) {
        System.out.println("[INFO] [" + tag + "] " + s);
    }

    /** Logs a warning message. */
    public static void warn(String s) {
        System.out.println("[WARN] " + s);
    }

    /** Logs a tagged warning message. */
    public static void warn(String tag, String s) {
        System.out.println("[WARN] [" + tag + "] " + s);
    }

    /** Logs an error message. */
    public static void error(String s) {
        System.err.println("[ERROR] " + s);
    }

    /** Logs a tagged error message. */
    public static void error(String tag, String s) {
        System.err.println("[ERROR] [" + tag + "] " + s);
    }

    /** Logs an error message with a stack trace. */
    public static void error(String s, Throwable t) {
        System.err.println("[ERROR] " + s);
        t.printStackTrace(System.err);
    }

    /** Logs a tagged error message with a stack trace. */
    public static void error(String tag, String s, Throwable t) {
        System.err.println("[ERROR] [" + tag + "] " + s);
        t.printStackTrace(System.err);
    }
}
