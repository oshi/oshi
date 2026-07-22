/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.openbsd;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.sun.jna.Memory;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.jna.util.SysctlUtilJNA;

/**
 * Provides access to sysctl calls on OpenBSD
 */
@ThreadSafe
public final class OpenBsdSysctlUtil {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdSysctlUtil.class);

    private OpenBsdSysctlUtil() {
    }

    /**
     * Executes a sysctl call with an int result
     *
     * @param name MIB array identifying the sysctl
     * @param def  default int value
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(int[] name, int def) {
        return SysctlUtilJNA.sysctl(
                (oldp, oldlenp) -> OpenBsdLibc.INSTANCE.sysctl(name, name.length, oldp, oldlenp, null, size_t.ZERO),
                Arrays.toString(name), def, LOG, true);
    }

    /**
     * Executes a sysctl call with a long result
     *
     * @param name MIB array identifying the sysctl
     * @param def  default long value
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(int[] name, long def) {
        return SysctlUtilJNA.sysctl(
                (oldp, oldlenp) -> OpenBsdLibc.INSTANCE.sysctl(name, name.length, oldp, oldlenp, null, size_t.ZERO),
                Arrays.toString(name), def, LOG);
    }

    /**
     * Executes a sysctl call with a String result
     *
     * @param name MIB array identifying the sysctl
     * @param def  default String value
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(int[] name, String def) {
        return SysctlUtilJNA.sysctl(
                (oldp, oldlenp) -> OpenBsdLibc.INSTANCE.sysctl(name, name.length, oldp, oldlenp, null, size_t.ZERO),
                Arrays.toString(name), def, LOG, true);
    }

    /**
     * Executes a sysctl call with a Structure result
     *
     * @param name   MIB array identifying the sysctl
     * @param struct structure for the result
     * @return True if structure is successfuly populated, false otherwise
     */
    public static boolean sysctl(int[] name, Structure struct) {
        return SysctlUtilJNA.sysctl(
                (oldp, oldlenp) -> OpenBsdLibc.INSTANCE.sysctl(name, name.length, oldp, oldlenp, null, size_t.ZERO),
                Arrays.toString(name), struct, LOG, Level.ERROR);
    }

    /**
     * Executes a sysctl call with a Pointer result
     *
     * @param name MIB array identifying the sysctl
     * @return An allocated memory buffer containing the result on success, null otherwise. Its value on failure is
     *         undefined.
     */
    public static Memory sysctl(int[] name) {
        return SysctlUtilJNA.sysctl(
                (oldp, oldlenp) -> OpenBsdLibc.INSTANCE.sysctl(name, name.length, oldp, oldlenp, null, size_t.ZERO),
                Arrays.toString(name), LOG, Level.ERROR);
    }
}
