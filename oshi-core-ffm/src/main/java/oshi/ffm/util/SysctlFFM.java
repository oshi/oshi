/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaBooleanOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaLongOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.LogLevel;
import oshi.util.ParseUtil;

/**
 * Shared implementation of the FFM {@code sysctl} result handling used by the per-platform utility classes. The only
 * per-platform difference is the native call itself (macOS and FreeBSD use {@code sysctlbyname(String)}; OpenBSD uses
 * {@code sysctl(int[])}); callers supply that call as a {@link SysctlOp} so the arena management, buffer sizing, and
 * result decoding live in one place. The {@code SysctlOp} is responsible for allocating its own name/MIB segment,
 * invoking the native function, and logging any errno on failure.
 */
@ThreadSafe
public final class SysctlFFM {

    /** All supported platforms define {@code size_t} as an unsigned 64-bit value. */
    public static final ValueLayout.OfLong SIZE_T = JAVA_LONG;

    /**
     * Logged when a sysctl call fails. Parameterized rather than concatenated so the name is only formatted on the
     * failure path, and shared so the message is identical across every result type.
     */
    private static final String FAILURE_MESSAGE = "Failed to get sysctl value for {}";

    private SysctlFFM() {
    }

    /**
     * A single native {@code sysctl}/{@code sysctlbyname} invocation. Implementations allocate their name or MIB
     * segment from the supplied arena, invoke the native function, log any errno on failure, and report success.
     */
    @FunctionalInterface
    public interface SysctlOp {
        /**
         * Invokes the bound native sysctl call.
         *
         * @param arena   the confined arena for any scratch allocations
         * @param oldp    buffer to receive the result, or {@link MemorySegment#NULL} to query the required size
         * @param oldlenp in/out size of {@code oldp}
         * @return {@code true} on success; {@code false} on failure
         * @throws Throwable on FFM invocation error. Implementations call {@link java.lang.invoke.MethodHandle}
         *                   {@code invokeExact}, which declares {@code Throwable}, so nothing narrower can be declared
         *                   here.
         */
        boolean invoke(Arena arena, MemorySegment oldp, MemorySegment oldlenp) throws Throwable; // NOSONAR java:S112
    }

    /**
     * Executes a sysctl call with an int result.
     *
     * @param op   the bound native sysctl call
     * @param def  default int value
     * @param log  logger for the calling class
     * @param name name of the sysctl, for logging
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(SysctlOp op, int def, Logger log, Object name) {
        return callInArenaIntOrDefault(arena -> {
            MemorySegment valueSeg = arena.allocate(JAVA_INT);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, JAVA_INT.byteSize());
            if (!op.invoke(arena, valueSeg, sizeSeg)) {
                return def;
            }
            return valueSeg.get(JAVA_INT, 0);
        }, def, log, LogLevel.WARN, FAILURE_MESSAGE, name);
    }

    /**
     * Executes a sysctl call with a long result.
     *
     * @param op   the bound native sysctl call
     * @param def  default long value
     * @param log  logger for the calling class
     * @param name name of the sysctl, for logging
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(SysctlOp op, long def, Logger log, Object name) {
        return callInArenaLongOrDefault(arena -> {
            MemorySegment valueSeg = arena.allocate(JAVA_LONG);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, JAVA_LONG.byteSize());
            if (!op.invoke(arena, valueSeg, sizeSeg)) {
                return def;
            }
            // Some OIDs exposed as a "long" are actually kernel ints (e.g. FreeBSD hw.clockrate is a 4-byte int).
            // sysctl updates oldlenp in place with the number of bytes actually written, so read only that many,
            // widening a 4-byte int as unsigned.
            return sizeSeg.get(SIZE_T, 0) == JAVA_INT.byteSize()
                    ? ParseUtil.unsignedIntToLong(valueSeg.get(JAVA_INT, 0))
                    : valueSeg.get(JAVA_LONG, 0);
        }, def, log, LogLevel.WARN, FAILURE_MESSAGE, name);
    }

    /**
     * Executes a sysctl call with a String result.
     *
     * @param op   the bound native sysctl call
     * @param def  default String value
     * @param log  logger for the calling class
     * @param name name of the sysctl, for logging
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(SysctlOp op, String def, Logger log, Object name) {
        return callInArenaOrDefault(arena -> {
            MemorySegment sizeSeg = arena.allocate(SIZE_T);
            if (!op.invoke(arena, MemorySegment.NULL, sizeSeg)) {
                return def;
            }
            long size = sizeSeg.get(SIZE_T, 0);
            MemorySegment valueSeg = arena.allocate(size + 1);
            if (!op.invoke(arena, valueSeg, sizeSeg)) {
                return def;
            }
            return valueSeg.getString(0);
        }, def, log, LogLevel.WARN, FAILURE_MESSAGE, name);
    }

    /**
     * Executes a sysctl call that populates a caller-provided struct segment.
     *
     * @param op     the bound native sysctl call
     * @param struct segment sized to the expected struct layout
     * @param log    logger for the calling class
     * @param name   name of the sysctl, for logging
     * @return {@code true} if the struct was populated, {@code false} otherwise
     */
    public static boolean sysctl(SysctlOp op, MemorySegment struct, Logger log, Object name) {
        return callInArenaBooleanOrDefault(arena -> {
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, struct.byteSize());
            return op.invoke(arena, struct, sizeSeg);
        }, false, log, LogLevel.WARN, FAILURE_MESSAGE, name);
    }

    /**
     * Executes a sysctl call returning a freshly allocated buffer of the natural size.
     *
     * @param op   the bound native sysctl call
     * @param log  logger for the calling class
     * @param name name of the sysctl, for logging
     * @return an auto-arena {@link MemorySegment} containing the result on success; {@code null} otherwise
     */
    public static @Nullable MemorySegment sysctl(SysctlOp op, Logger log, Object name) {
        return callInArenaOrDefault(arena -> {
            MemorySegment sizeSeg = arena.allocate(SIZE_T);
            if (!op.invoke(arena, MemorySegment.NULL, sizeSeg)) {
                return null;
            }
            long size = sizeSeg.get(SIZE_T, 0);
            MemorySegment valueSeg = arena.allocate(size);
            if (!op.invoke(arena, valueSeg, sizeSeg)) {
                return null;
            }
            MemorySegment returnSeg = Arena.ofAuto().allocate(size);
            returnSeg.copyFrom(valueSeg);
            return returnSeg;
        }, null, log, LogLevel.WARN, FAILURE_MESSAGE, name);
    }
}
