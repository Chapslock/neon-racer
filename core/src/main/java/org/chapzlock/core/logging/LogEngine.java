package org.chapzlock.core.logging;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogEngine {

    private static LogLevel globalLevel = resolveGlobalLevel();
    private static final Map<String, LogLevel> classOverrides = new ConcurrentHashMap<>();
    private static long startNanos = System.nanoTime();
    private static PrintStream out = System.out;
    private static PrintStream err = System.err;

    private static final String[] LEVEL_LABELS = {"TRACE", "DEBUG", "INFO ", "WARN ", "ERROR"};

    static LogLevel resolveGlobalLevel() {
        String prop = System.getProperty("engine.log", "INFO");
        try {
            return LogLevel.valueOf(prop.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LogLevel.INFO;
        }
    }

    public static void setGlobalLevel(LogLevel level) {
        globalLevel = level;
    }

    public static LogLevel getGlobalLevel() {
        return globalLevel;
    }

    public static LogLevel getEffectiveLevel(String loggerName) {
        LogLevel override = classOverrides.get(loggerName);
        return override != null ? override : globalLevel;
    }

    public static void setClassLevel(String loggerName, LogLevel level) {
        classOverrides.put(loggerName, level);
    }

    public static void clearClassLevel(String loggerName) {
        classOverrides.remove(loggerName);
    }

    public static void emit(LogLevel level, String source, String message) {
        float elapsed = (System.nanoTime() - startNanos) / 1_000_000_000f;
        String line = String.format("[%.3fs] [%s] [%s] %s",
            elapsed, LEVEL_LABELS[level.ordinal()], source, message);
        if (level == LogLevel.ERROR) {
            err.println(line);
        } else {
            out.println(line);
        }
    }

    public static void emit(LogLevel level, String source, String message, Throwable t) {
        emit(level, source, message);
        t.printStackTrace(err);
    }

    public static void resetClock() {
        startNanos = System.nanoTime();
    }

    public static void setOutput(PrintStream stdout, PrintStream stderr) {
        out = stdout;
        err = stderr;
    }

    public static void resetOutput() {
        out = System.out;
        err = System.err;
    }

    static void loadClassOverrides() {
        System.getProperties().forEach((key, value) -> {
            String k = key.toString();
            if (k.startsWith("engine.log.")) {
                String className = k.substring("engine.log.".length());
                try {
                    classOverrides.put(className, LogLevel.valueOf(value.toString().toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
        });
    }

    static {
        loadClassOverrides();
    }
}
