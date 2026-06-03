/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.freebsd;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.ffm.unix.freebsd.FreeBsdLibcFunctions.SIZE_T;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.unix.freebsd.FreeBsdLibcFunctions;

/**
 * Provides access to sysctl calls on FreeBSD via the FFM API.
 * <p>
 * Mirrors the API of {@code oshi.util.platform.unix.freebsd.BsdSysctlUtil} (JNA) so per-class FFM concretes can be
 * swapped in without changing call sites.
 */
@ThreadSafe
public final class BsdSysctlUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(BsdSysctlUtilFFM.class);

    private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

    private BsdSysctlUtilFFM() {
    }

    /**
     * Executes a sysctl call with an int result.
     *
     * @param name name of the sysctl
     * @param def  default int value
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(String name, int def) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment valueSeg = arena.allocate(JAVA_INT);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, JAVA_INT.byteSize());
            if (!sysctlbyname(arena, nameSeg, valueSeg, sizeSeg)) {
                return def;
            }
            return valueSeg.get(JAVA_INT, 0);
        } catch (Throwable e) {
            LOG.warn("Failed to get sysctl value for {}", name, e);
            return def;
        }
    }

    /**
     * Executes a sysctl call with a long result.
     *
     * @param name name of the sysctl
     * @param def  default long value
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(String name, long def) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment valueSeg = arena.allocate(JAVA_LONG);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, JAVA_LONG.byteSize());
            if (!sysctlbyname(arena, nameSeg, valueSeg, sizeSeg)) {
                return def;
            }
            return valueSeg.get(JAVA_LONG, 0);
        } catch (Throwable e) {
            LOG.warn("Failed to get sysctl value for {}", name, e);
            return def;
        }
    }

    /**
     * Executes a sysctl call with a String result.
     *
     * @param name name of the sysctl
     * @param def  default String value
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(String name, String def) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment sizeSeg = arena.allocate(SIZE_T);
            if (!sysctlbyname(arena, nameSeg, MemorySegment.NULL, sizeSeg)) {
                return def;
            }
            long size = sizeSeg.get(SIZE_T, 0);
            // +1 for null terminator
            MemorySegment valueSeg = arena.allocate(size + 1);
            if (!sysctlbyname(arena, nameSeg, valueSeg, sizeSeg)) {
                return def;
            }
            return valueSeg.getString(0);
        } catch (Throwable e) {
            LOG.warn("Failed to get sysctl value for {}", name, e);
            return def;
        }
    }

    /**
     * Executes a sysctl call that populates a caller-provided struct segment.
     *
     * @param name   name of the sysctl
     * @param struct segment sized to the expected struct layout
     * @return {@code true} if the struct was populated, {@code false} otherwise
     */
    public static boolean sysctl(String name, MemorySegment struct) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, struct.byteSize());
            return sysctlbyname(arena, nameSeg, struct, sizeSeg);
        } catch (Throwable e) {
            LOG.warn("Failed to get sysctl value for {}", name, e);
            return false;
        }
    }

    /**
     * Executes a sysctl call returning a freshly allocated buffer of the natural size.
     *
     * @param name name of the sysctl
     * @return an auto-arena {@link MemorySegment} containing the result on success; {@code null} otherwise
     */
    public static MemorySegment sysctl(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment sizeSeg = arena.allocate(SIZE_T);
            if (!sysctlbyname(arena, nameSeg, MemorySegment.NULL, sizeSeg)) {
                return null;
            }
            long size = sizeSeg.get(SIZE_T, 0);
            MemorySegment valueSeg = arena.allocate(size);
            if (!sysctlbyname(arena, nameSeg, valueSeg, sizeSeg)) {
                return null;
            }
            // Copy to an auto-arena segment so it outlives this confined arena
            MemorySegment returnSeg = Arena.ofAuto().allocate(size);
            returnSeg.copyFrom(valueSeg);
            return returnSeg;
        } catch (Throwable e) {
            LOG.warn("Failed to get sysctl value for {}", name, e);
            return null;
        }
    }

    private static boolean sysctlbyname(Arena arena, MemorySegment name, MemorySegment oldp, MemorySegment oldlenp)
            throws Throwable {
        MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
        int result = FreeBsdLibcFunctions.sysctlbyname(callState, name, oldp, oldlenp, MemorySegment.NULL, 0L);
        if (result != 0) {
            LOG.warn(SYSCTL_FAIL, name.getString(0), getErrno(callState));
            return false;
        }
        return true;
    }
}
