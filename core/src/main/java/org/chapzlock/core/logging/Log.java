package org.chapzlock.core.logging;

import lombok.experimental.UtilityClass;

/**
 * Simple logging utility for the engine.
 *
 * <p>Levels: {@code debug}, {@code info}, {@code warn}, {@code error}.
 * Debug logging is controlled by {@code -Dengine.debug=true} or {@link #setDebugEnabled(boolean)}.
 */
@UtilityClass
public class Log {

    private static boolean debugEnabled = Boolean.getBoolean("engine.debug");

    /** Enables or disables debug logging. */
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

    /** Logs an info message. */
    public static void info(String s) {
        System.out.println("[INFO] " + s);
    }

    /** Logs a warning message. */
    public static void warn(String s) {
        System.out.println("[WARN] " + s);
    }

    /** Logs an error message. */
    public static void error(String s) {
        System.err.println("[ERROR] " + s);
    }

    /** Logs an error message with a stack trace. */
    public static void error(String s, Throwable t) {
        System.err.println("[ERROR] " + s);
        t.printStackTrace(System.err);
    }
}
