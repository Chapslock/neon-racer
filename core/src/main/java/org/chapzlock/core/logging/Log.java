package org.chapzlock.core.logging;

/**
 * Per-class logger for the engine.
 *
 * <p>Create one instance per class as a static field:
 * <pre>{@code
 * private static final Log log = Log.create(MySystem.class);
 * log.info("Scene loaded in %dms", elapsed);
 * }</pre>
 *
 * <p>Five levels, from most to least verbose: TRACE, DEBUG, INFO, WARN, ERROR.
 * Configure the global level with {@code -Dengine.log=DEBUG} and per-class
 * overrides with {@code -Dengine.log.PhysicsSystem=TRACE}.
 */
public class Log {

    private final String name;

    private Log(String name) {
        this.name = name;
    }

    public static Log create(Class<?> clazz) {
        return new Log(clazz.getSimpleName());
    }

    public void trace(String message) {
        if (isEnabled(LogLevel.TRACE)) {
            LogEngine.emit(LogLevel.TRACE, name, message);
        }
    }

    public void trace(String format, Object... args) {
        if (isEnabled(LogLevel.TRACE)) {
            LogEngine.emit(LogLevel.TRACE, name, String.format(format, args));
        }
    }

    public void debug(String message) {
        if (isEnabled(LogLevel.DEBUG)) {
            LogEngine.emit(LogLevel.DEBUG, name, message);
        }
    }

    public void debug(String format, Object... args) {
        if (isEnabled(LogLevel.DEBUG)) {
            LogEngine.emit(LogLevel.DEBUG, name, String.format(format, args));
        }
    }

    public void info(String message) {
        if (isEnabled(LogLevel.INFO)) {
            LogEngine.emit(LogLevel.INFO, name, message);
        }
    }

    public void info(String format, Object... args) {
        if (isEnabled(LogLevel.INFO)) {
            LogEngine.emit(LogLevel.INFO, name, String.format(format, args));
        }
    }

    public void warn(String message) {
        if (isEnabled(LogLevel.WARN)) {
            LogEngine.emit(LogLevel.WARN, name, message);
        }
    }

    public void warn(String format, Object... args) {
        if (isEnabled(LogLevel.WARN)) {
            LogEngine.emit(LogLevel.WARN, name, String.format(format, args));
        }
    }

    public void error(String message) {
        if (isEnabled(LogLevel.ERROR)) {
            LogEngine.emit(LogLevel.ERROR, name, message);
        }
    }

    public void error(String format, Object... args) {
        if (isEnabled(LogLevel.ERROR)) {
            LogEngine.emit(LogLevel.ERROR, name, String.format(format, args));
        }
    }

    public void error(String message, Throwable t) {
        if (isEnabled(LogLevel.ERROR)) {
            LogEngine.emit(LogLevel.ERROR, name, message, t);
        }
    }

    public boolean isTraceEnabled() {
        return isEnabled(LogLevel.TRACE);
    }

    public boolean isDebugEnabled() {
        return isEnabled(LogLevel.DEBUG);
    }

    private boolean isEnabled(LogLevel level) {
        return level.isEnabled(LogEngine.getEffectiveLevel(name));
    }
}
