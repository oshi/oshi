/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.netbsd;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.NetBsdLibc;
import oshi.jna.util.SysctlUtilJNA;
import oshi.util.LogLevel;

/**
 * Provides access to native sysctl calls on NetBSD via JNA.
 * <p>
 * JNA does not distribute a native binary for NetBSD; it is only available as an optional pkgsrc package
 * ({@code java-jna}), and OSHI does not require it to be installed. Callers must therefore gate every native call on
 * {@link #JNA_AVAILABLE} and fall back to the command-line implementations in
 * {@code oshi.util.common.platform.unix.bsd.BsdSysctlUtil} when it is {@code false}.
 */
@ThreadSafe
public final class NetBsdSysctlUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NetBsdSysctlUtil.class);

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
        return SysctlUtilJNA.sysctl(
                (oldp, oldlenp) -> NetBsdLibc.INSTANCE.sysctl(name, name.length, oldp, oldlenp, null, size_t.ZERO),
                Arrays.toString(name), def, LOG, true);
    }

    /**
     * Executes a sysctl call with a long result.
     *
     * @param name MIB array identifying the sysctl
     * @param def  default long value
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(int[] name, long def) {
        return SysctlUtilJNA.sysctl(
                (oldp, oldlenp) -> NetBsdLibc.INSTANCE.sysctl(name, name.length, oldp, oldlenp, null, size_t.ZERO),
                Arrays.toString(name), def, LOG);
    }

    /**
     * Executes a sysctl call with a String result.
     *
     * @param name MIB array identifying the sysctl
     * @param def  default String value
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(int[] name, String def) {
        return SysctlUtilJNA.sysctl(
                (oldp, oldlenp) -> NetBsdLibc.INSTANCE.sysctl(name, name.length, oldp, oldlenp, null, size_t.ZERO),
                Arrays.toString(name), def, LOG, true);
    }

    /**
     * Executes a sysctl call with a Structure result.
     *
     * @param name   MIB array identifying the sysctl
     * @param struct structure for the result
     * @return True if structure is successfully populated, false otherwise
     */
    public static boolean sysctl(int[] name, Structure struct) {
        return SysctlUtilJNA.sysctl(
                (oldp, oldlenp) -> NetBsdLibc.INSTANCE.sysctl(name, name.length, oldp, oldlenp, null, size_t.ZERO),
                Arrays.toString(name), struct, LOG, LogLevel.ERROR);
    }

    /**
     * Executes a sysctl call with a Pointer result.
     *
     * @param name MIB array identifying the sysctl
     * @return An allocated memory buffer containing the result on success, null otherwise. Its value on failure is
     *         undefined.
     */
    public static Memory sysctl(int[] name) {
        return SysctlUtilJNA.sysctl(
                (oldp, oldlenp) -> NetBsdLibc.INSTANCE.sysctl(name, name.length, oldp, oldlenp, null, size_t.ZERO),
                Arrays.toString(name), LOG, LogLevel.ERROR);
    }
}
