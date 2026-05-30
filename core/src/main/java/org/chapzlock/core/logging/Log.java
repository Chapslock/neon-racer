package org.chapzlock.core.logging;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Log {

    private static boolean debugEnabled = Boolean.getBoolean("engine.debug");

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void debug(String s) {
        if (debugEnabled) {
            System.out.println("[DEBUG] " + s);
        }
    }

    public static void info(String s) {
        System.out.println("[INFO] " + s);
    }

    public static void warn(String s) {
        System.out.println("[WARN] " + s);
    }

    public static void error(String s) {
        System.err.println("[ERROR] " + s);
    }

    public static void error(String s, Throwable t) {
        System.err.println("[ERROR] " + s);
        t.printStackTrace(System.err);
    }
}
