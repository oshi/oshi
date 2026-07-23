/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * The level at which {@link ExceptionUtil} logs a swallowed exception.
 * <p>
 * OSHI carries the level with this enum rather than {@code org.slf4j.event.Level} so that it never class-loads a type
 * introduced in slf4j-api 1.7.15. {@code ExceptionUtil} dispatches to the classic
 * {@code Logger#debug/info/warn/error/trace} methods (present since slf4j 1.0), which keeps OSHI loggable against
 * host-provided slf4j-api versions older than 1.7.15 (e.g. the 1.7.5 bundled by Apache Maven 3.3.x, which loads
 * OSHI-based extensions against its own slf4j).
 */
@ThreadSafe
public enum LogLevel {
    /**
     * The TRACE level.
     */
    TRACE,
    /**
     * The DEBUG level.
     */
    DEBUG,
    /**
     * The INFO level.
     */
    INFO,
    /**
     * The WARN level.
     */
    WARN,
    /**
     * The ERROR level.
     */
    ERROR
}
