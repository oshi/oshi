/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.util;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.util.ParseUtil;

/**
 * Shared implementation of the JNA {@code sysctl} result handling used by the per-platform utility classes. The only
 * per-platform difference is the native call itself (macOS and FreeBSD use {@code sysctlbyname(String)}; NetBSD and
 * OpenBSD use {@code sysctl(int[])}); callers supply that call as a {@link SysctlCall} so the buffer sizing, error
 * logging, and result decoding live in one place.
 */
@ThreadSafe
public final class SysctlUtilJNA {

    private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

    private static final int INT_SIZE = Native.getNativeSize(int.class);

    private static final int UINT64_SIZE = Native.getNativeSize(long.class);

    private SysctlUtilJNA() {
    }

    /**
     * A single native {@code sysctl}/{@code sysctlbyname} invocation with the name (String or MIB array) already bound
     * by the caller.
     */
    @FunctionalInterface
    public interface SysctlCall {
        /**
         * Invokes the bound native sysctl call.
         *
         * @param oldp    buffer to receive the result, or {@code null} to query the required size
         * @param oldlenp in/out size of {@code oldp}
         * @return 0 on success; nonzero on failure (with {@code errno} set)
         */
        int call(Pointer oldp, size_t.ByReference oldlenp);
    }

    /**
     * Executes a sysctl call with an int result.
     *
     * @param call       the bound native sysctl call
     * @param name       name of the sysctl, for logging
     * @param def        default int value
     * @param log        logger for the calling class
     * @param logWarning whether to log a warning if the call fails
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(SysctlCall call, Object name, int def, Logger log, boolean logWarning) {
        try (Memory p = new Memory(INT_SIZE);
                CloseableSizeTByReference size = new CloseableSizeTByReference(INT_SIZE)) {
            if (0 != call.call(p, size)) {
                if (logWarning) {
                    log.warn(SYSCTL_FAIL, name, Native.getLastError());
                }
                return def;
            }
            return p.getInt(0);
        }
    }

    /**
     * Executes a sysctl call with a long result.
     *
     * @param call the bound native sysctl call
     * @param name name of the sysctl, for logging
     * @param def  default long value
     * @param log  logger for the calling class
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(SysctlCall call, Object name, long def, Logger log) {
        try (Memory p = new Memory(UINT64_SIZE);
                CloseableSizeTByReference size = new CloseableSizeTByReference(UINT64_SIZE)) {
            if (0 != call.call(p, size)) {
                log.warn(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            // Some OIDs exposed as a "long" are actually kernel ints (e.g. FreeBSD hw.clockrate is a 4-byte int). The
            // kernel then writes only 4 bytes into the 8-byte buffer, and reading getLong(0) would combine the 4 valid
            // bytes with 4 uninitialized ones. sysctl updates oldlenp in place with the number of bytes actually
            // written, so read only that many, widening a 4-byte int as unsigned (matching an over-wide read of a
            // zeroed buffer).
            return size.longValue() == INT_SIZE ? ParseUtil.unsignedIntToLong(p.getInt(0)) : p.getLong(0);
        }
    }

    /**
     * Executes a sysctl call with a String result.
     *
     * @param call       the bound native sysctl call
     * @param name       name of the sysctl, for logging
     * @param def        default String value
     * @param log        logger for the calling class
     * @param logWarning whether to log a warning if the call fails
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(SysctlCall call, Object name, String def, Logger log, boolean logWarning) {
        // Call first time with null pointer to get value of size
        try (CloseableSizeTByReference size = new CloseableSizeTByReference()) {
            if (0 != call.call(null, size)) {
                if (logWarning) {
                    log.warn(SYSCTL_FAIL, name, Native.getLastError());
                }
                return def;
            }
            // Add 1 to size for null terminated string
            try (Memory p = new Memory(size.longValue() + 1L)) {
                if (0 != call.call(p, size)) {
                    if (logWarning) {
                        log.warn(SYSCTL_FAIL, name, Native.getLastError());
                    }
                    return def;
                }
                return p.getString(0);
            }
        }
    }

    /**
     * Executes a sysctl call that populates a caller-provided {@link Structure}.
     *
     * @param call   the bound native sysctl call
     * @param name   name of the sysctl, for logging
     * @param struct structure for the result
     * @param log    logger for the calling class
     * @param level  level at which to log a failure
     * @return {@code true} if the structure was successfully populated, {@code false} otherwise
     */
    public static boolean sysctl(SysctlCall call, Object name, Structure struct, Logger log, Level level) {
        try (CloseableSizeTByReference size = new CloseableSizeTByReference(struct.size())) {
            if (0 != call.call(struct.getPointer(), size)) {
                logFailure(log, level, name);
                return false;
            }
        }
        struct.read();
        return true;
    }

    /**
     * Executes a sysctl call returning a freshly allocated buffer of the natural size.
     *
     * @param call  the bound native sysctl call
     * @param name  name of the sysctl, for logging
     * @param log   logger for the calling class
     * @param level level at which to log a failure
     * @return An allocated memory buffer containing the result on success, {@code null} otherwise. Its value on failure
     *         is undefined.
     */
    public static Memory sysctl(SysctlCall call, Object name, Logger log, Level level) {
        try (CloseableSizeTByReference size = new CloseableSizeTByReference()) {
            if (0 != call.call(null, size)) {
                logFailure(log, level, name);
                return null;
            }
            Memory m = new Memory(size.longValue());
            if (0 != call.call(m, size)) {
                logFailure(log, level, name);
                m.close();
                return null;
            }
            return m;
        }
    }

    private static void logFailure(Logger log, Level level, Object name) {
        if (level == Level.ERROR) {
            log.error(SYSCTL_FAIL, name, Native.getLastError());
        } else {
            log.warn(SYSCTL_FAIL, name, Native.getLastError());
        }
    }
}
