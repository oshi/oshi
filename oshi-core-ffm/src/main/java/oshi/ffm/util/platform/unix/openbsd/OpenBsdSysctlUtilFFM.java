/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.openbsd;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaBooleanOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaLongOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.SIZE_T;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;

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
        return callInArenaIntOrDefault(arena -> {
            MemorySegment valueSeg = arena.allocate(JAVA_INT);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, JAVA_INT.byteSize());
            if (!sysctlMib(arena, mib, valueSeg, sizeSeg)) {
                return def;
            }
            return valueSeg.get(JAVA_INT, 0);
        }, LOG, Level.WARN, "Failed to get sysctl value for " + Arrays.toString(mib), def);
    }

    /**
     * Executes a sysctl call with a long result.
     *
     * @param mib MIB array identifying the sysctl
     * @param def default long value
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(int[] mib, long def) {
        return callInArenaLongOrDefault(arena -> {
            MemorySegment valueSeg = arena.allocate(JAVA_LONG);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, JAVA_LONG.byteSize());
            if (!sysctlMib(arena, mib, valueSeg, sizeSeg)) {
                return def;
            }
            return valueSeg.get(JAVA_LONG, 0);
        }, LOG, Level.WARN, "Failed to get sysctl value for " + Arrays.toString(mib), def);
    }

    /**
     * Executes a sysctl call with a String result.
     *
     * @param mib MIB array identifying the sysctl
     * @param def default String value
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(int[] mib, String def) {
        return callInArenaOrDefault(arena -> {
            MemorySegment sizeSeg = arena.allocate(SIZE_T);
            if (!sysctlMib(arena, mib, MemorySegment.NULL, sizeSeg)) {
                return def;
            }
            long size = sizeSeg.get(SIZE_T, 0);
            MemorySegment valueSeg = arena.allocate(size + 1);
            if (!sysctlMib(arena, mib, valueSeg, sizeSeg)) {
                return def;
            }
            return valueSeg.getString(0);
        }, LOG, Level.WARN, "Failed to get sysctl value for " + Arrays.toString(mib), def);
    }

    /**
     * Executes a sysctl call that populates a caller-provided struct segment.
     *
     * @param mib    MIB array identifying the sysctl
     * @param struct segment sized to the expected struct layout
     * @return {@code true} if the struct was populated, {@code false} otherwise
     */
    public static boolean sysctl(int[] mib, MemorySegment struct) {
        return callInArenaBooleanOrDefault(arena -> {
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, struct.byteSize());
            return sysctlMib(arena, mib, struct, sizeSeg);
        }, LOG, Level.WARN, "Failed to get sysctl value for " + Arrays.toString(mib), false);
    }

    /**
     * Executes a sysctl call returning a freshly allocated buffer of the natural size.
     *
     * @param mib MIB array identifying the sysctl
     * @return an auto-arena {@link MemorySegment} containing the result on success; {@code null} otherwise
     */
    public static MemorySegment sysctl(int[] mib) {
        return callInArenaOrDefault(arena -> {
            MemorySegment sizeSeg = arena.allocate(SIZE_T);
            if (!sysctlMib(arena, mib, MemorySegment.NULL, sizeSeg)) {
                return null;
            }
            long size = sizeSeg.get(SIZE_T, 0);
            MemorySegment valueSeg = arena.allocate(size);
            if (!sysctlMib(arena, mib, valueSeg, sizeSeg)) {
                return null;
            }
            MemorySegment returnSeg = Arena.ofAuto().allocate(size);
            returnSeg.copyFrom(valueSeg);
            return returnSeg;
        }, LOG, Level.WARN, "Failed to get sysctl value for " + Arrays.toString(mib), null);
    }

    private static boolean sysctlMib(Arena arena, int[] mib, MemorySegment oldp, MemorySegment oldlenp)
            throws Throwable {
        MemorySegment mibSeg = arena.allocate(JAVA_INT, mib.length);
        for (int i = 0; i < mib.length; i++) {
            mibSeg.setAtIndex(JAVA_INT, i, mib[i]);
        }
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
