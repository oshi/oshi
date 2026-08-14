/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import org.slf4j.Logger;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility methods for logging at a {@link LogLevel} chosen at runtime.
 */
@ThreadSafe
public final class LogUtil {

    private LogUtil() {
    }

    /**
     * Tests whether a level is enabled for a logger, using only SLF4J 1.x-compatible {@link Logger} methods. This is
     * the equivalent of slf4j 2.x's {@code Logger.isEnabledForLevel}, whose parameter type OSHI cannot reference (see
     * {@link LogLevel}).
     * <p>
     * {@link #logAtLevel(Logger, LogLevel, String, Object...)} does not need this guard: SLF4J checks the level itself
     * before substituting {@code {}} placeholders. Use this only to skip work the caller must perform <em>before</em>
     * the logging call, such as formatting an argument.
     *
     * @param log   the logger to query
     * @param level the level to test
     * @return true if the level is enabled for the logger
     */
    public static boolean isEnabled(Logger log, LogLevel level) {
        switch (level) {
            case ERROR:
                return log.isErrorEnabled();
            case WARN:
                return log.isWarnEnabled();
            case INFO:
                return log.isInfoEnabled();
            case TRACE:
                return log.isTraceEnabled();
            case DEBUG:
            default:
                return log.isDebugEnabled();
        }
    }

    /**
     * Logs a message at the given level using only SLF4J 1.x-compatible {@link Logger} methods (no {@code atLevel}
     * fluent API).
     * <p>
     * The message is formatted by SLF4J only if the level is enabled, so callers do not need to guard this call with
     * {@code isDebugEnabled()} and friends. Use {@code {}} placeholders rather than concatenation so that argument
     * substitution remains deferred. Guard with {@link #isEnabled(Logger, LogLevel)} only when computing an argument is
     * itself expensive.
     * <p>
     * SLF4J's implicit-cause handling applies: when the last argument is a {@link Throwable} with no matching
     * placeholder, it is attached as the log event's cause rather than substituted into the format string.
     *
     * @param log   the logger to use
     * @param level the level at which to log
     * @param msg   the log message, with {@code {}} placeholders for the arguments
     * @param args  the arguments to substitute into the message
     */
    // Forwarding the caller's message is the entire point of this method, so the format string cannot be a constant
    // here; the callers are the ones that must pass one.
    @SuppressWarnings("Slf4jFormatShouldBeConst")
    public static void logAtLevel(Logger log, LogLevel level, String msg, Object... args) {
        switch (level) {
            case ERROR:
                log.error(msg, args);
                break;
            case WARN:
                log.warn(msg, args);
                break;
            case INFO:
                log.info(msg, args);
                break;
            case TRACE:
                log.trace(msg, args);
                break;
            case DEBUG:
            default:
                log.debug(msg, args);
                break;
        }
    }
}
