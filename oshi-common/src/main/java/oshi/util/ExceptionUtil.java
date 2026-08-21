/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.lang.invoke.WrongMethodTypeException;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility methods for reducing repetitive exception handling boilerplate, particularly around FFM (Foreign Function and
 * Memory) native calls that require catching {@link Throwable}.
 * <p>
 * Catching {@link Throwable} is necessary here because {@code MethodHandle.invokeExact} declares it, but it also
 * catches {@link WrongMethodTypeException} — a defect in a native binding rather than a failed system call. Every
 * wrapper below therefore treats that one as unrecoverable: it is logged at {@code ERROR}, whatever level (or silence)
 * the caller asked for, and then rethrown rather than absorbed into a default value. It is unchecked, so this changes
 * no signature; the reasoning is on {@link #logCaught}.
 */
@ThreadSafe
public final class ExceptionUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionUtil.class);

    /**
     * Appended to the caller's message when the throwable is a {@link WrongMethodTypeException}. Carries no {@code {}}
     * placeholder, so the caller's arguments still line up with the message it supplied.
     */
    private static final String SIGNATURE_MISMATCH = " -- the native binding's declared signature does not match its"
            + " call site. This is a defect in OSHI rather than a condition on this machine: it fails identically for"
            + " every caller and every input, so it is rethrown instead of being reported as missing data. Please"
            + " report it.";

    private ExceptionUtil() {
    }

    /**
     * Logs a caught throwable at the caller's level and returns, or logs a {@link WrongMethodTypeException} at
     * {@code ERROR} and rethrows it.
     * <p>
     * {@code invokeExact} is signature-polymorphic: the call site states the signature, so a mismatch with the handle's
     * descriptor throws {@link WrongMethodTypeException} rather than failing to compile. One exception covers the whole
     * signature — return type, parameter types, boxing and arity alike — and its message prints both sides, so it is
     * rethrown unaltered. That is never recoverable and never data-dependent: it fails on the first call and on every
     * call after it, on every machine. Yet it arrives here as just another {@link Throwable}, and returning the default
     * value for it has hidden the bug three times: issues #3301 and #3422, and a Windows connector lookup that reported
     * the sentinel for every display until a cross-implementation test happened to compare the two backends' values.
     * <p>
     * A default value is the right answer to "this machine cannot answer" and the wrong answer to "this binding is
     * wrong", which is why the two are separated here. Rethrowing surfaces the defect at the offending call site, in
     * the test run for the platform the binding belongs to, rather than leaving it to whoever reads the log. The
     * {@code ERROR} line is logged first so the wrapper's own message survives as context; where wrappers nest, each
     * layer logs one line as the exception passes through.
     * <p>
     * Public so other modules can apply the same policy from their own catch blocks; {@code oshi-core-ffm}'s
     * {@code ForeignFunctions} wrappers funnel through it. Use it instead of
     * {@link #logAtLevel(Logger, LogLevel, String, Throwable, Object...)} wherever the throwable was caught from a
     * native call rather than constructed locally.
     *
     * @param log   the logger to use
     * @param level the level at which to log an ordinary failure
     * @param msg   the log message (use {} for each argument, and none for the exception)
     * @param t     the throwable that was caught
     * @param args  the arguments filling the {} placeholders, if any
     */
    public static void logCaught(Logger log, LogLevel level, String msg, Throwable t, @Nullable Object... args) {
        if (t instanceof WrongMethodTypeException) {
            logAtLevel(log, LogLevel.ERROR, msg + SIGNATURE_MISMATCH, t, args);
            throw (WrongMethodTypeException) t;
        }
        logAtLevel(log, level, msg, t, args);
    }

    /**
     * Applies the same treatment on behalf of the wrappers that have no logger of their own and are documented to
     * swallow failures, and does nothing for any other throwable. A binding defect is worth breaking that silence for;
     * an expected native failure is not. See {@link #logCaught}.
     */
    private static void handleSignatureMismatch(Throwable t) {
        if (t instanceof WrongMethodTypeException) {
            logAtLevel(LOG, LogLevel.ERROR, "A native call failed" + SIGNATURE_MISMATCH, t);
            throw (WrongMethodTypeException) t;
        }
    }

    /**
     * Logs a throwable at the given level, attaching the throwable itself as the log event's cause via SLF4J's
     * implicit-cause handling. Level dispatch is delegated to
     * {@link LogUtil#logAtLevel(Logger, LogLevel, String, Object...)}.
     * <p>
     * The caller's arguments fill the {@code {}} placeholders in order, so {@code msg} should carry exactly one
     * {@code {}} per argument and none for the exception: the throwable is rendered by the logging backend, which
     * reports its message and stack trace along with the cause chain. For example,
     * {@code logAtLevel(log, DEBUG, "Failed to read {}", t, path)} logs the path followed by the exception.
     * <p>
     * Public so other modules (e.g., {@code oshi-core-ffm}) can share this dispatch instead of duplicating it.
     *
     * @param log   the logger to use
     * @param level the level at which to log
     * @param msg   the log message (use {} for each argument, and none for the exception)
     * @param t     the throwable to log
     * @param args  the arguments filling the {} placeholders, if any
     */
    public static void logAtLevel(Logger log, LogLevel level, String msg, Throwable t, @Nullable Object... args) {
        // Append the throwable itself; SLF4J takes a trailing throwable as the cause rather than a substitution
        // argument, so it is logged with its stack trace instead of being formatted into the message. Copy into an
        // Object[] explicitly: a caller may pass a typed array (e.g. String[]) for the varargs, and a copy keeping
        // that component type would throw ArrayStoreException on the write below.
        Object[] all = Arrays.copyOf(args, args.length + 1, Object[].class);
        all[args.length] = t;
        LogUtil.logAtLevel(log, level, msg, all);
    }

    /**
     * A supplier that may throw any {@link Throwable}, including checked exceptions.
     *
     * @param <T> the type of result supplied
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T extends @Nullable Object> {
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
    public static <T extends @Nullable Object> T getOrDefault(ThrowingSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            handleSignatureMismatch(t);
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
     * @param msg          the log message (use {} for each argument, and none for the exception)
     * @param args         the arguments filling the {} placeholders, if any
     * @return the supplier's result or the default value
     */
    public static <T extends @Nullable Object> T getOrDefault(ThrowingSupplier<T> supplier, T defaultValue, Logger log,
            String msg, Object... args) {
        return getOrDefault(supplier, defaultValue, log, LogLevel.DEBUG, msg, args);
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
     * @param msg          the log message (use {} for each argument, and none for the exception)
     * @param args         the arguments filling the {} placeholders, if any
     * @return the supplier's result or the default value
     */
    public static <T extends @Nullable Object> T getOrDefault(ThrowingSupplier<T> supplier, T defaultValue, Logger log,
            LogLevel level, String msg, Object... args) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            logCaught(log, level, msg, t, args);
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
            handleSignatureMismatch(t);
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
     * @param msg          the log message (use {} for each argument, and none for the exception)
     * @param args         the arguments filling the {} placeholders, if any
     * @return the supplier's result or the default value
     */
    public static int getIntOrDefault(ThrowingIntSupplier supplier, int defaultValue, Logger log, String msg,
            Object... args) {
        return getIntOrDefault(supplier, defaultValue, log, LogLevel.DEBUG, msg, args);
    }

    /**
     * Executes the int supplier, returning its result on success or the default value on failure. Logs the exception at
     * the specified level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param level        the level at which to log the exception
     * @param msg          the log message (use {} for each argument, and none for the exception)
     * @param args         the arguments filling the {} placeholders, if any
     * @return the supplier's result or the default value
     */
    public static int getIntOrDefault(ThrowingIntSupplier supplier, int defaultValue, Logger log, LogLevel level,
            String msg, Object... args) {
        try {
            return supplier.getAsInt();
        } catch (Throwable t) {
            logCaught(log, level, msg, t, args);
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
            handleSignatureMismatch(t);
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
     * @param msg          the log message (use {} for each argument, and none for the exception)
     * @param args         the arguments filling the {} placeholders, if any
     * @return the supplier's result or the default value
     */
    public static long getLongOrDefault(ThrowingLongSupplier supplier, long defaultValue, Logger log, String msg,
            Object... args) {
        return getLongOrDefault(supplier, defaultValue, log, LogLevel.DEBUG, msg, args);
    }

    /**
     * Executes the long supplier, returning its result on success or the default value on failure. Logs the exception
     * at the specified level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param level        the level at which to log the exception
     * @param msg          the log message (use {} for each argument, and none for the exception)
     * @param args         the arguments filling the {} placeholders, if any
     * @return the supplier's result or the default value
     */
    public static long getLongOrDefault(ThrowingLongSupplier supplier, long defaultValue, Logger log, LogLevel level,
            String msg, Object... args) {
        try {
            return supplier.getAsLong();
        } catch (Throwable t) {
            logCaught(log, level, msg, t, args);
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
            handleSignatureMismatch(t);
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
     * @param msg          the log message (use {} for each argument, and none for the exception)
     * @param args         the arguments filling the {} placeholders, if any
     * @return the supplier's result or the default value
     */
    public static boolean getBooleanOrDefault(ThrowingBooleanSupplier supplier, boolean defaultValue, Logger log,
            String msg, Object... args) {
        return getBooleanOrDefault(supplier, defaultValue, log, LogLevel.DEBUG, msg, args);
    }

    /**
     * Executes the boolean supplier, returning its result on success or the default value on failure. Logs the
     * exception at the specified level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param level        the level at which to log the exception
     * @param msg          the log message (use {} for each argument, and none for the exception)
     * @param args         the arguments filling the {} placeholders, if any
     * @return the supplier's result or the default value
     */
    public static boolean getBooleanOrDefault(ThrowingBooleanSupplier supplier, boolean defaultValue, Logger log,
            LogLevel level, String msg, Object... args) {
        try {
            return supplier.getAsBoolean();
        } catch (Throwable t) {
            logCaught(log, level, msg, t, args);
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
            handleSignatureMismatch(t);
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
     * @param msg          the log message (use {} for each argument, and none for the exception)
     * @param args         the arguments filling the {} placeholders, if any
     * @return the supplier's result or the default value
     */
    public static double getDoubleOrDefault(ThrowingDoubleSupplier supplier, double defaultValue, Logger log,
            String msg, Object... args) {
        return getDoubleOrDefault(supplier, defaultValue, log, LogLevel.DEBUG, msg, args);
    }

    /**
     * Executes the double supplier, returning its result on success or the default value on failure. Logs the exception
     * at the specified level.
     *
     * @param supplier     the operation to attempt
     * @param defaultValue the value to return on failure
     * @param log          the logger to use
     * @param level        the level at which to log the exception
     * @param msg          the log message (use {} for each argument, and none for the exception)
     * @param args         the arguments filling the {} placeholders, if any
     * @return the supplier's result or the default value
     */
    public static double getDoubleOrDefault(ThrowingDoubleSupplier supplier, double defaultValue, Logger log,
            LogLevel level, String msg, Object... args) {
        try {
            return supplier.getAsDouble();
        } catch (Throwable t) {
            logCaught(log, level, msg, t, args);
            return defaultValue;
        }
    }

    /**
     * Executes the supplier, wrapping the result in an {@link Optional}. Returns {@link Optional#empty()} on failure.
     *
     * @param <T>      the result type
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param msg      the log message (use {} for each argument, and none for the exception)
     * @param args     the arguments filling the {} placeholders, if any
     * @return an Optional containing the result, or empty on failure
     */
    public static <T> Optional<T> getOptional(ThrowingSupplier<? extends @Nullable T> supplier, Logger log, String msg,
            Object... args) {
        return getOptional(supplier, log, LogLevel.DEBUG, msg, args);
    }

    /**
     * Executes the supplier, wrapping the result in an {@link Optional}. Returns {@link Optional#empty()} on failure.
     * Logs the exception at the specified level.
     *
     * @param <T>      the result type
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param level    the level at which to log the exception
     * @param msg      the log message (use {} for each argument, and none for the exception)
     * @param args     the arguments filling the {} placeholders, if any
     * @return an Optional containing the result, or empty on failure
     */
    public static <T> Optional<T> getOptional(ThrowingSupplier<? extends @Nullable T> supplier, Logger log,
            LogLevel level, String msg, Object... args) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (Throwable t) {
            logCaught(log, level, msg, t, args);
            return Optional.empty();
        }
    }

    /**
     * Executes the int supplier, wrapping the result in an {@link OptionalInt}. Returns {@link OptionalInt#empty()} on
     * failure.
     *
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param msg      the log message (use {} for each argument, and none for the exception)
     * @param args     the arguments filling the {} placeholders, if any
     * @return an OptionalInt containing the result, or empty on failure
     */
    public static OptionalInt getOptionalInt(ThrowingIntSupplier supplier, Logger log, String msg, Object... args) {
        return getOptionalInt(supplier, log, LogLevel.DEBUG, msg, args);
    }

    /**
     * Executes the int supplier, wrapping the result in an {@link OptionalInt}. Returns {@link OptionalInt#empty()} on
     * failure. Logs the exception at the specified level.
     *
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param level    the level at which to log the exception
     * @param msg      the log message (use {} for each argument, and none for the exception)
     * @param args     the arguments filling the {} placeholders, if any
     * @return an OptionalInt containing the result, or empty on failure
     */
    public static OptionalInt getOptionalInt(ThrowingIntSupplier supplier, Logger log, LogLevel level, String msg,
            Object... args) {
        try {
            return OptionalInt.of(supplier.getAsInt());
        } catch (Throwable t) {
            logCaught(log, level, msg, t, args);
            return OptionalInt.empty();
        }
    }

    /**
     * Executes the long supplier, wrapping the result in an {@link OptionalLong}. Returns {@link OptionalLong#empty()}
     * on failure.
     *
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param msg      the log message (use {} for each argument, and none for the exception)
     * @param args     the arguments filling the {} placeholders, if any
     * @return an OptionalLong containing the result, or empty on failure
     */
    public static OptionalLong getOptionalLong(ThrowingLongSupplier supplier, Logger log, String msg, Object... args) {
        return getOptionalLong(supplier, log, LogLevel.DEBUG, msg, args);
    }

    /**
     * Executes the long supplier, wrapping the result in an {@link OptionalLong}. Returns {@link OptionalLong#empty()}
     * on failure. Logs the exception at the specified level.
     *
     * @param supplier the operation to attempt
     * @param log      the logger to use
     * @param level    the level at which to log the exception
     * @param msg      the log message (use {} for each argument, and none for the exception)
     * @param args     the arguments filling the {} placeholders, if any
     * @return an OptionalLong containing the result, or empty on failure
     */
    public static OptionalLong getOptionalLong(ThrowingLongSupplier supplier, Logger log, LogLevel level, String msg,
            Object... args) {
        try {
            return OptionalLong.of(supplier.getAsLong());
        } catch (Throwable t) {
            logCaught(log, level, msg, t, args);
            return OptionalLong.empty();
        }
    }

    /**
     * Executes the runnable, silently swallowing any {@link Throwable} — except a {@link WrongMethodTypeException},
     * which is logged and rethrown as a binding defect.
     *
     * @param runnable the operation to attempt
     */
    public static void runSilently(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            // intentionally silent, unless the binding itself is wrong
            handleSignatureMismatch(t);
        }
    }

    /**
     * Executes the runnable, logging any {@link Throwable} at debug level.
     *
     * @param runnable the operation to attempt
     * @param log      the logger to use
     * @param msg      the log message (use {} for each argument, and none for the exception)
     * @param args     the arguments filling the {} placeholders, if any
     */
    public static void runOrLog(ThrowingRunnable runnable, Logger log, String msg, Object... args) {
        runOrLog(runnable, log, LogLevel.DEBUG, msg, args);
    }

    /**
     * Executes the runnable, logging any {@link Throwable} at the specified level.
     *
     * @param runnable the operation to attempt
     * @param log      the logger to use
     * @param level    the level at which to log the exception
     * @param msg      the log message (use {} for each argument, and none for the exception)
     * @param args     the arguments filling the {} placeholders, if any
     */
    public static void runOrLog(ThrowingRunnable runnable, Logger log, LogLevel level, String msg, Object... args) {
        try {
            runnable.run();
        } catch (Throwable t) {
            logCaught(log, level, msg, t, args);
        }
    }
}
