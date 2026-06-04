package org.chapzlock.core.logging;

public enum LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR;

    public boolean isEnabled(LogLevel threshold) {
        return this.ordinal() >= threshold.ordinal();
    }
}
