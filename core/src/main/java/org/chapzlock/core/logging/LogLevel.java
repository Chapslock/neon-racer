package org.chapzlock.core.logging;

/**
 * Log severity levels in increasing order of importance.
 *
 * <ul>
 *   <li>{@link #TRACE} — extremely detailed diagnostic output (e.g., per-frame values)</li>
 *   <li>{@link #DEBUG} — developer-facing info useful during development (e.g., asset loading)</li>
 *   <li>{@link #INFO} — notable runtime events (e.g., scene loaded, engine started)</li>
 *   <li>{@link #WARN} — recoverable problems that may indicate misconfiguration</li>
 *   <li>{@link #ERROR} — failures that prevent correct operation</li>
 * </ul>
 *
 * @see LogEngine
 */
public enum LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR;

    public boolean isEnabled(LogLevel threshold) {
        return this.ordinal() >= threshold.ordinal();
    }
}
