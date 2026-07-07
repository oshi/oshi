/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.netbsd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.NetBsdLibc;

/**
 * Provides access to native sysctl calls on NetBSD via JNA.
 * <p>
 * JNA does not distribute a native binary for NetBSD; it is only available as an optional pkgsrc package
 * ({@code java-jna}), and OSHI does not require it to be installed. Callers must therefore gate every native call on
 * {@link #JNA_AVAILABLE} and fall back to the command-line implementations in
 * {@code oshi.util.common.platform.unix.netbsd.NetBsdSysctlUtil} when it is {@code false}.
 */
@ThreadSafe
public final class NetBsdSysctlUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NetBsdSysctlUtil.class);

    private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

    /**
     * Whether the JNA native library is available on this system. When {@code false}, the native methods in this class
     * will not function and callers must use the command-line fallbacks. Detected once at class load by forcing the
     * {@link NetBsdLibc} native library to load and making a trivial call.
     */
    public static final boolean JNA_AVAILABLE;
    static {
        boolean available = false;
        try {
            // Forcing INSTANCE triggers loading of the JNA native dispatch library and the NetBSD libc binding, which
            // throws UnsatisfiedLinkError (wrapped in ExceptionInInitializerError on first access) when the java-jna
            // package is not installed. A trivial call confirms the binding is actually functional.
            available = NetBsdLibc.INSTANCE.getpid() >= 0;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError | ExceptionInInitializerError e) {
            LOG.info("JNA native library is not available on NetBSD; using command-line fallbacks.");
        }
        JNA_AVAILABLE = available;
    }

    private NetBsdSysctlUtil() {
    }

    /**
     * Executes a sysctl call with an int result.
     *
     * @param name MIB array identifying the sysctl
     * @param def  default int value
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(int[] name, int def) {
        int intSize = NetBsdLibc.INT_SIZE;
        try (Memory p = new Memory(intSize); CloseableSizeTByReference size = new CloseableSizeTByReference(intSize)) {
            if (0 != NetBsdLibc.INSTANCE.sysctl(name, name.length, p, size, null, size_t.ZERO)) {
                LOG.warn(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            return p.getInt(0);
        }
    }

    /**
     * Executes a sysctl call with a long result.
     *
     * @param name MIB array identifying the sysctl
     * @param def  default long value
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(int[] name, long def) {
        int uint64Size = NetBsdLibc.UINT64_SIZE;
        try (Memory p = new Memory(uint64Size);
                CloseableSizeTByReference size = new CloseableSizeTByReference(uint64Size)) {
            if (0 != NetBsdLibc.INSTANCE.sysctl(name, name.length, p, size, null, size_t.ZERO)) {
                LOG.warn(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            return p.getLong(0);
        }
    }

    /**
     * Executes a sysctl call with a String result.
     *
     * @param name MIB array identifying the sysctl
     * @param def  default String value
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(int[] name, String def) {
        // Call first time with null pointer to get value of size
        try (CloseableSizeTByReference size = new CloseableSizeTByReference()) {
            if (0 != NetBsdLibc.INSTANCE.sysctl(name, name.length, null, size, null, size_t.ZERO)) {
                LOG.warn(SYSCTL_FAIL, name, Native.getLastError());
                return def;
            }
            // Add 1 to size for null terminated string
            try (Memory p = new Memory(size.longValue() + 1L)) {
                if (0 != NetBsdLibc.INSTANCE.sysctl(name, name.length, p, size, null, size_t.ZERO)) {
                    LOG.warn(SYSCTL_FAIL, name, Native.getLastError());
                    return def;
                }
                return p.getString(0);
            }
        }
    }

    /**
     * Executes a sysctl call with a Structure result.
     *
     * @param name   MIB array identifying the sysctl
     * @param struct structure for the result
     * @return True if structure is successfully populated, false otherwise
     */
    public static boolean sysctl(int[] name, Structure struct) {
        try (CloseableSizeTByReference size = new CloseableSizeTByReference(struct.size())) {
            if (0 != NetBsdLibc.INSTANCE.sysctl(name, name.length, struct.getPointer(), size, null, size_t.ZERO)) {
                LOG.error(SYSCTL_FAIL, name, Native.getLastError());
                return false;
            }
        }
        struct.read();
        return true;
    }

    /**
     * Executes a sysctl call with a Pointer result.
     *
     * @param name MIB array identifying the sysctl
     * @return An allocated memory buffer containing the result on success, null otherwise. Its value on failure is
     *         undefined.
     */
    public static Memory sysctl(int[] name) {
        try (CloseableSizeTByReference size = new CloseableSizeTByReference()) {
            if (0 != NetBsdLibc.INSTANCE.sysctl(name, name.length, null, size, null, size_t.ZERO)) {
                LOG.error(SYSCTL_FAIL, name, Native.getLastError());
                return null;
            }
            Memory m = new Memory(size.longValue());
            if (0 != NetBsdLibc.INSTANCE.sysctl(name, name.length, m, size, null, size_t.ZERO)) {
                LOG.error(SYSCTL_FAIL, name, Native.getLastError());
                m.close();
                return null;
            }
            return m;
        }
    }
}
