/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.openbsd;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.getErrno;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;
import oshi.ffm.util.SysctlFFM;

/**
 * Provides access to sysctl calls on OpenBSD via the FFM API.
 * <p>
 * Mirrors the API of {@code oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil} (JNA) so per-class FFM concretes can be
 * swapped in without changing call sites.
 */
@ThreadSafe
public final class OpenBsdSysctlUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdSysctlUtilFFM.class);

    private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

    private OpenBsdSysctlUtilFFM() {
    }

    /**
     * Executes a sysctl call with an int result.
     *
     * @param mib MIB array identifying the sysctl
     * @param def default int value
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(int[] mib, int def) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlMib(arena, mib, oldp, oldlenp), def, LOG,
                Arrays.toString(mib));
    }

    /**
     * Executes a sysctl call with a long result.
     *
     * @param mib MIB array identifying the sysctl
     * @param def default long value
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(int[] mib, long def) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlMib(arena, mib, oldp, oldlenp), def, LOG,
                Arrays.toString(mib));
    }

    /**
     * Executes a sysctl call with a String result.
     *
     * @param mib MIB array identifying the sysctl
     * @param def default String value
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(int[] mib, String def) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlMib(arena, mib, oldp, oldlenp), def, LOG,
                Arrays.toString(mib));
    }

    /**
     * Executes a sysctl call that populates a caller-provided struct segment.
     *
     * @param mib    MIB array identifying the sysctl
     * @param struct segment sized to the expected struct layout
     * @return {@code true} if the struct was populated, {@code false} otherwise
     */
    public static boolean sysctl(int[] mib, MemorySegment struct) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlMib(arena, mib, oldp, oldlenp), struct, LOG,
                Arrays.toString(mib));
    }

    /**
     * Executes a sysctl call returning a freshly allocated buffer of the natural size.
     *
     * @param mib MIB array identifying the sysctl
     * @return an auto-arena {@link MemorySegment} containing the result on success; {@code null} otherwise
     */
    public static MemorySegment sysctl(int[] mib) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlMib(arena, mib, oldp, oldlenp), LOG,
                Arrays.toString(mib));
    }

    private static boolean sysctlMib(Arena arena, int[] mib, MemorySegment oldp, MemorySegment oldlenp)
            throws Throwable {
        MemorySegment mibSeg = arena.allocateFrom(JAVA_INT, mib);
        MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
        int result = OpenBsdLibcFunctions.sysctl(callState, mibSeg, mib.length, oldp, oldlenp, MemorySegment.NULL, 0L);
        if (result != 0) {
            // Guard so the native getErrno read is skipped when WARN is disabled
            if (LOG.isWarnEnabled()) {
                LOG.warn(SYSCTL_FAIL, Arrays.toString(mib), getErrno(callState));
            }
            return false;
        }
        return true;
    }
}
