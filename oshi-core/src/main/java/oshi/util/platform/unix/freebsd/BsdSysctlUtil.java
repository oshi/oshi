/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.freebsd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.FreeBsdLibc;

/**
 * Provides access to sysctl calls on FreeBSD
 */
@ThreadSafe
public final class BsdSysctlUtil {

    private static final Logger LOG = LoggerFactory.getLogger(BsdSysctlUtil.class);

    private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

    private BsdSysctlUtil() {
    }

    /**
     * Executes a sysctl call with an int result
     *
     * @param name name of the sysctl
     * @param def  default int value
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(String name, int def) {
        int intSize = FreeBsdLibc.INT_SIZE;
        try (Memory p = new Memory(intSize); CloseableSizeTByReference size = new CloseableSizeTByReference(intSize)) {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, size_t.ZERO)) {
                LOG.warn(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            return p.getInt(0);
        }
    }

    /**
     * Executes a sysctl call with a long result
     *
     * @param name name of the sysctl
     * @param def  default long value
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(String name, long def) {
        int uint64Size = FreeBsdLibc.UINT64_SIZE;
        try (Memory p = new Memory(uint64Size);
                CloseableSizeTByReference size = new CloseableSizeTByReference(uint64Size)) {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, size_t.ZERO)) {
                LOG.warn(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            return p.getLong(0);
        }
    }

    /**
     * Executes a sysctl call with a String result
     *
     * @param name name of the sysctl
     * @param def  default String value
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(String name, String def) {
        // Call first time with null pointer to get value of size
        try (CloseableSizeTByReference size = new CloseableSizeTByReference()) {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, null, size, null, size_t.ZERO)) {
                LOG.warn(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            // Add 1 to size for null terminated string
            try (Memory p = new Memory(size.longValue() + 1L)) {
                if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, size_t.ZERO)) {
                    LOG.warn(SYSCTL_FAIL, name, Native.getLastError());
                    return def;
                }
                return p.getString(0);
            }
        }
    }

    /**
     * Executes a sysctl call with a Structure result
     *
     * @param name   name of the sysctl
     * @param struct structure for the result
     * @return True if structure is successfuly populated, false otherwise
     */
    public static boolean sysctl(String name, Structure struct) {
        try (CloseableSizeTByReference size = new CloseableSizeTByReference(struct.size())) {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, struct.getPointer(), size, null, size_t.ZERO)) {
                LOG.error(SYSCTL_FAIL, name, Native.getLastError());
                return false;
            }
        }
        struct.read();
        return true;
    }

    /**
     * Executes a sysctl call with a Pointer result
     *
     * @param name name of the sysctl
     * @return An allocated memory buffer containing the result on success, null otherwise. Its value on failure is
     *         undefined.
     */
    public static Memory sysctl(String name) {
        try (CloseableSizeTByReference size = new CloseableSizeTByReference()) {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, null, size, null, size_t.ZERO)) {
                LOG.error(SYSCTL_FAIL, name, Native.getLastError());
                return null;
            }
            Memory m = new Memory(size.longValue());
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, m, size, null, size_t.ZERO)) {
                LOG.error(SYSCTL_FAIL, name, Native.getLastError());
                m.close();
                return null;
            }
            return m;
        }
    }
}
