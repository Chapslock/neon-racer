package org.chapzlock.core.logging;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Log {
    public static void info(String s) {
        System.out.println(s);
    }

    public static void warn(String s) {
        System.out.println("[WARN] " + s);
    }

    public static void error(String s) {
        System.err.println(s);
    }
}
