/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.slf4j.Logger;

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
        T get() throws Throwable;
    }

    /**
     * An int-returning supplier that may throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingIntSupplier {
        int getAsInt() throws Throwable;
    }

    /**
     * A long-returning supplier that may throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingLongSupplier {
        long getAsLong() throws Throwable;
    }

    /**
     * A boolean-returning supplier that may throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingBooleanSupplier {
        boolean getAsBoolean() throws Throwable;
    }

    /**
     * A double-returning supplier that may throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingDoubleSupplier {
        double getAsDouble() throws Throwable;
    }

    /**
     * A runnable that may throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
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
        try {
            return supplier.get();
        } catch (Throwable t) {
            log.debug(msg, t);
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
        try {
            return supplier.getAsInt();
        } catch (Throwable t) {
            log.debug(msg, t);
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
        try {
            return supplier.getAsLong();
        } catch (Throwable t) {
            log.debug(msg, t);
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
        try {
            return supplier.getAsBoolean();
        } catch (Throwable t) {
            log.debug(msg, t);
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
        try {
            return supplier.getAsDouble();
        } catch (Throwable t) {
            log.debug(msg, t);
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
        try {
            return Optional.ofNullable(supplier.get());
        } catch (Throwable t) {
            log.debug(msg, t);
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
        try {
            return OptionalInt.of(supplier.getAsInt());
        } catch (Throwable t) {
            log.debug(msg, t);
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
        try {
            return OptionalLong.of(supplier.getAsLong());
        } catch (Throwable t) {
            log.debug(msg, t);
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
        try {
            runnable.run();
        } catch (Throwable t) {
            log.debug(msg, t);
        }
    }
}
