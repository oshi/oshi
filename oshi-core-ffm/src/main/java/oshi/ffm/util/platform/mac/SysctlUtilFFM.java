/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.mac;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaLongOrDefault;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.ffm.util.SysctlFFM.SIZE_T;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.mac.MacSystemFunctions;
import oshi.ffm.util.SysctlFFM;
import oshi.util.LogLevel;

/**
 * Provides access to sysctl calls on macOS
 */
@ThreadSafe
public final class SysctlUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(SysctlUtilFFM.class);

    private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

    private SysctlUtilFFM() {
    }

    /**
     * Executes a sysctl call with an int result
     *
     * @param name name of the sysctl
     * @param def  default int value
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(String name, int def) {
        return sysctl(name, def, true);
    }

    /**
     * Executes a sysctl call with an int result
     *
     * @param name       name of the sysctl
     * @param def        default int value
     * @param logWarning whether to log the warning if not available
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(String name, int def, boolean logWarning) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlbyname(arena, name, oldp, oldlenp, logWarning), def,
                LOG, name);
    }

    /**
     * Executes a sysctl call with a long result
     *
     * @param name name of the sysctl
     * @param def  default long value
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(String name, long def) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlbyname(arena, name, oldp, oldlenp, true), def, LOG,
                name);
    }

    /**
     * Executes a sysctl call with a String result
     *
     * @param name name of the sysctl
     * @param def  default String value
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(String name, String def) {
        return sysctl(name, def, true);
    }

    /**
     * Executes a sysctl call with a String result
     *
     * @param name       name of the sysctl
     * @param def        default String value
     * @param logWarning whether to log the warning if not available
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(String name, String def, boolean logWarning) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlbyname(arena, name, oldp, oldlenp, logWarning), def,
                LOG, name);
    }

    /**
     * Executes a sysctl call with a Structure result
     *
     * @param name   name of the sysctl
     * @param struct structure for the result
     * @return True if structure is successfuly populated, false otherwise
     */
    public static boolean sysctl(String name, MemorySegment struct) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlbyname(arena, name, oldp, oldlenp, true), struct, LOG,
                name);
    }

    /**
     * Executes a sysctl call with a Pointer result
     *
     * @param name name of the sysctl
     * @return An allocated memory buffer containing the result on success, null otherwise. Its value on failure is
     *         undefined.
     */
    public static MemorySegment sysctl(String name) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlbyname(arena, name, oldp, oldlenp, true), LOG, name);
    }

    /**
     * Executes a sysctl call with a Memory Segment result
     *
     * @param mib    definition of the sysctl
     * @param buffer buffer to hold the result. Must be allocated
     * @return The size of data written to the buffer, or -1 if the call failed
     */
    public static long sysctl(int[] mib, MemorySegment buffer) {
        return callInArenaLongOrDefault(arena -> {
            MemorySegment mibSeg = arena.allocateFrom(JAVA_INT, mib);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, buffer.byteSize());
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            int result = MacSystemFunctions.sysctl(callState, mibSeg, mib.length, buffer, sizeSeg, MemorySegment.NULL,
                    0L);

            if (result != 0) {
                LOG.warn(SYSCTL_FAIL, Arrays.toString(mib), getErrno(callState));
                return -1L;
            }
            return sizeSeg.get(SIZE_T, 0);
        }, LOG, LogLevel.WARN, "Failed to get sysctl value for " + Arrays.toString(mib), -1L);
    }

    private static boolean sysctlbyname(Arena arena, String name, MemorySegment oldp, MemorySegment oldlenp,
            boolean logWarning) throws Throwable {
        MemorySegment nameSeg = arena.allocateFrom(name);
        MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
        int result = MacSystemFunctions.sysctlbyname(callState, nameSeg, oldp, oldlenp, MemorySegment.NULL, 0L);
        if (result != 0) {
            if (logWarning) {
                LOG.warn(SYSCTL_FAIL, name, getErrno(callState));
            }
            return false;
        }
        return true;
    }
}
