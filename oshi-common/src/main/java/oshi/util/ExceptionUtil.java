/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility methods for reducing repetitive exception handling boilerplate, particularly around FFM (Foreign Function and
 * Memory) native calls that require catching {@link Throwable}.
 */
@ThreadSafe
public final class ExceptionUtil {

    private ExceptionUtil() {
    }

    /**
     * A supplier that may throw any {@link Throwable}, including checked exceptions.
     *
     * @param <T> the type of result supplied
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        /**
         * Gets a result.
         *
         * @return a result
         * @throws Throwable if unable to compute
         */
        T get() throws Throwable;
    }

    /**
     * An int-returning supplier that may throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingIntSupplier {
        /**
         * Gets an int result.
         *
         * @return an int result
         * @throws Throwable if unable to compute
         */
        int getAsInt() throws Throwable;
    }

    /**
     * A long-returning supplier that may throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingLongSupplier {
        /**
         * Gets a long result.
         *
         * @return a long result
         * @throws Throwable if unable to compute
         */
        long getAsLong() throws Throwable;
    }

    /**
     * A boolean-returning supplier that may throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingBooleanSupplier {
        /**
         * Gets a boolean result.
         *
         * @return a boolean result
         * @throws Throwable if unable to compute
         */
        boolean getAsBoolean() throws Throwable;
    }

    /**
     * A double-returning supplier that may throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingDoubleSupplier {
        /**
         * Gets a double result.
         *
         * @return a double result
         * @throws Throwable if unable to compute
         */
        double getAsDouble() throws Throwable;
    }

    /**
     * A runnable that may throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        /**
         * Performs the operation.
         *
         * @throws Throwable if unable to perform
         */
        void run() throws Throwable;
    }

    /**
     * Executes the supplier, returning its result on success or the default value if any {@link Throwable} is thrown.
     *
     * @param <T>          the result type
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @return the supplier's result or the default value
     */
    public static <T> T getOrDefault(ThrowingSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    /**
     * Executes the supplier, returning its result on success or the default value if any {@link Throwable} is thrown.
     * Logs the exception at debug level.
     *
     * @param <T>          the result type
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param msg          the log message (use {} for the exception message placeholder)
     * @return the supplier's result or the default value
     */
    public static <T> T getOrDefault(ThrowingSupplier<T> supplier, T defaultValue, Logger log, String msg) {
        return getOrDefault(supplier, defaultValue, log, Level.DEBUG, msg);
    }

    /**
     * Executes the supplier, returning its result on success or the default value if any {@link Throwable} is thrown.
     * Logs the exception at the specified level.
     *
     * @param <T>          the result type
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param level        the level at which to log the exception
     * @param msg          the log message (use {} for the exception message placeholder)
     * @return the supplier's result or the default value
     */
    public static <T> T getOrDefault(ThrowingSupplier<T> supplier, T defaultValue, Logger log, Level level,
            String msg) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            log.atLevel(level).setCause(t).log(msg, t.getMessage());
            return defaultValue;
        }
    }

    /**
     * Executes the int supplier, returning its result on success or the default value on failure.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @return the supplier's result or the default value
     */
    public static int getIntOrDefault(ThrowingIntSupplier supplier, int defaultValue) {
        try {
            return supplier.getAsInt();
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    /**
     * Executes the int supplier, returning its result on success or the default value on failure. Logs the exception at
     * debug level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param msg          the log message
     * @return the supplier's result or the default value
     */
    public static int getIntOrDefault(ThrowingIntSupplier supplier, int defaultValue, Logger log, String msg) {
        return getIntOrDefault(supplier, defaultValue, log, Level.DEBUG, msg);
    }

    /**
     * Executes the int supplier, returning its result on success or the default value on failure. Logs the exception at
     * the specified level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param level        the level at which to log the exception
     * @param msg          the log message
     * @return the supplier's result or the default value
     */
    public static int getIntOrDefault(ThrowingIntSupplier supplier, int defaultValue, Logger log, Level level,
            String msg) {
        try {
            return supplier.getAsInt();
        } catch (Throwable t) {
            log.atLevel(level).setCause(t).log(msg, t.getMessage());
            return defaultValue;
        }
    }

    /**
     * Executes the long supplier, returning its result on success or the default value on failure.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @return the supplier's result or the default value
     */
    public static long getLongOrDefault(ThrowingLongSupplier supplier, long defaultValue) {
        try {
            return supplier.getAsLong();
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    /**
     * Executes the long supplier, returning its result on success or the default value on failure. Logs the exception
     * at debug level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param msg          the log message
     * @return the supplier's result or the default value
     */
    public static long getLongOrDefault(ThrowingLongSupplier supplier, long defaultValue, Logger log, String msg) {
        return getLongOrDefault(supplier, defaultValue, log, Level.DEBUG, msg);
    }

    /**
     * Executes the long supplier, returning its result on success or the default value on failure. Logs the exception
     * at the specified level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param level        the level at which to log the exception
     * @param msg          the log message
     * @return the supplier's result or the default value
     */
    public static long getLongOrDefault(ThrowingLongSupplier supplier, long defaultValue, Logger log, Level level,
            String msg) {
        try {
            return supplier.getAsLong();
        } catch (Throwable t) {
            log.atLevel(level).setCause(t).log(msg, t.getMessage());
            return defaultValue;
        }
    }

    /**
     * Executes the boolean supplier, returning its result on success or the default value on failure.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @return the supplier's result or the default value
     */
    public static boolean getBooleanOrDefault(ThrowingBooleanSupplier supplier, boolean defaultValue) {
        try {
            return supplier.getAsBoolean();
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    /**
     * Executes the boolean supplier, returning its result on success or the default value on failure. Logs the
     * exception at debug level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param msg          the log message
     * @return the supplier's result or the default value
     */
    public static boolean getBooleanOrDefault(ThrowingBooleanSupplier supplier, boolean defaultValue, Logger log,
            String msg) {
        return getBooleanOrDefault(supplier, defaultValue, log, Level.DEBUG, msg);
    }

    /**
     * Executes the boolean supplier, returning its result on success or the default value on failure. Logs the
     * exception at the specified level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param level        the level at which to log the exception
     * @param msg          the log message
     * @return the supplier's result or the default value
     */
    public static boolean getBooleanOrDefault(ThrowingBooleanSupplier supplier, boolean defaultValue, Logger log,
            Level level, String msg) {
        try {
            return supplier.getAsBoolean();
        } catch (Throwable t) {
            log.atLevel(level).setCause(t).log(msg, t.getMessage());
            return defaultValue;
        }
    }

    /**
     * Executes the double supplier, returning its result on success or the default value on failure.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @return the supplier's result or the default value
     */
    public static double getDoubleOrDefault(ThrowingDoubleSupplier supplier, double defaultValue) {
        try {
            return supplier.getAsDouble();
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    /**
     * Executes the double supplier, returning its result on success or the default value on failure. Logs the exception
     * at debug level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param msg          the log message
     * @return the supplier's result or the default value
     */
    public static double getDoubleOrDefault(ThrowingDoubleSupplier supplier, double defaultValue, Logger log,
            String msg) {
        return getDoubleOrDefault(supplier, defaultValue, log, Level.DEBUG, msg);
    }

    /**
     * Executes the double supplier, returning its result on success or the default value on failure. Logs the exception
     * at the specified level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param level        the level at which to log the exception
     * @param msg          the log message
     * @return the supplier's result or the default value
     */
    public static double getDoubleOrDefault(ThrowingDoubleSupplier supplier, double defaultValue, Logger log,
            Level level, String msg) {
        try {
            return supplier.getAsDouble();
        } catch (Throwable t) {
            log.atLevel(level).setCause(t).log(msg, t.getMessage());
            return defaultValue;
        }
    }

    /**
     * Executes the supplier, wrapping the result in an {@link Optional}. Returns {@link Optional#empty()} on failure.
     *
     * @param <T>      the result type
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param msg      the log message
     * @return an Optional containing the result, or empty on failure
     */
    public static <T> Optional<T> getOptional(ThrowingSupplier<T> supplier, Logger log, String msg) {
        return getOptional(supplier, log, Level.DEBUG, msg);
    }

    /**
     * Executes the supplier, wrapping the result in an {@link Optional}. Returns {@link Optional#empty()} on failure.
     * Logs the exception at the specified level.
     *
     * @param <T>      the result type
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param level    the level at which to log the exception
     * @param msg      the log message
     * @return an Optional containing the result, or empty on failure
     */
    public static <T> Optional<T> getOptional(ThrowingSupplier<T> supplier, Logger log, Level level, String msg) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (Throwable t) {
            log.atLevel(level).setCause(t).log(msg, t.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Executes the int supplier, wrapping the result in an {@link OptionalInt}. Returns {@link OptionalInt#empty()} on
     * failure.
     *
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param msg      the log message
     * @return an OptionalInt containing the result, or empty on failure
     */
    public static OptionalInt getOptionalInt(ThrowingIntSupplier supplier, Logger log, String msg) {
        return getOptionalInt(supplier, log, Level.DEBUG, msg);
    }

    /**
     * Executes the int supplier, wrapping the result in an {@link OptionalInt}. Returns {@link OptionalInt#empty()} on
     * failure. Logs the exception at the specified level.
     *
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param level    the level at which to log the exception
     * @param msg      the log message
     * @return an OptionalInt containing the result, or empty on failure
     */
    public static OptionalInt getOptionalInt(ThrowingIntSupplier supplier, Logger log, Level level, String msg) {
        try {
            return OptionalInt.of(supplier.getAsInt());
        } catch (Throwable t) {
            log.atLevel(level).setCause(t).log(msg, t.getMessage());
            return OptionalInt.empty();
        }
    }

    /**
     * Executes the long supplier, wrapping the result in an {@link OptionalLong}. Returns {@link OptionalLong#empty()}
     * on failure.
     *
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param msg      the log message
     * @return an OptionalLong containing the result, or empty on failure
     */
    public static OptionalLong getOptionalLong(ThrowingLongSupplier supplier, Logger log, String msg) {
        return getOptionalLong(supplier, log, Level.DEBUG, msg);
    }

    /**
     * Executes the long supplier, wrapping the result in an {@link OptionalLong}. Returns {@link OptionalLong#empty()}
     * on failure. Logs the exception at the specified level.
     *
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param level    the level at which to log the exception
     * @param msg      the log message
     * @return an OptionalLong containing the result, or empty on failure
     */
    public static OptionalLong getOptionalLong(ThrowingLongSupplier supplier, Logger log, Level level, String msg) {
        try {
            return OptionalLong.of(supplier.getAsLong());
        } catch (Throwable t) {
            log.atLevel(level).setCause(t).log(msg, t.getMessage());
            return OptionalLong.empty();
        }
    }

    /**
     * Executes the runnable, silently swallowing any {@link Throwable}.
     *
     * @param runnable the operation to attempt
     */
    public static void runSilently(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            // intentionally silent
        }
    }

    /**
     * Executes the runnable, logging any {@link Throwable} at debug level.
     *
     * @param runnable the operation to attempt
     * @param log      the logger to use
     * @param msg      the log message
     */
    public static void runOrLog(ThrowingRunnable runnable, Logger log, String msg) {
        runOrLog(runnable, log, Level.DEBUG, msg);
    }

    /**
     * Executes the runnable, logging any {@link Throwable} at the specified level.
     *
     * @param runnable the operation to attempt
     * @param log      the logger to use
     * @param level    the level at which to log the exception
     * @param msg      the log message
     */
    public static void runOrLog(ThrowingRunnable runnable, Logger log, Level level, String msg) {
        try {
            runnable.run();
        } catch (Throwable t) {
            log.atLevel(level).setCause(t).log(msg, t.getMessage());
        }
    }
}
