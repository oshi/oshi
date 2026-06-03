/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.unix.freebsd;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for FreeBSD libc functions used by OSHI.
 * <p>
 * Phase 3 scope covers the bindings consumed by {@link oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM} and the
 * first FFM concretes that need them: {@code sysctlbyname} and {@code getloadavg}. Additional libc bindings
 * ({@code sysctl} with int MIB, {@code getpid}, {@code thr_self}, {@code getrlimit}, {@code getutxent}) are added in
 * later phases alongside their first FFM consumer.
 */
public final class FreeBsdLibcFunctions extends ForeignFunctions {

    private FreeBsdLibcFunctions() {
    }

    /** Layout of the C {@code size_t} type on FreeBSD (8 bytes on all supported 64-bit archs). */
    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    // libc is already loaded into the JVM process; defaultLookup() avoids the libc.so vs libc.so.7 versioning pitfall
    // that breaks SymbolLookup.libraryLookup("c") on FreeBSD.
    private static final SymbolLookup LIBC = LINKER.defaultLookup();

    // int sysctlbyname(const char *name, void *oldp, size_t *oldlenp, void *newp, size_t newlen);
    private static final MethodHandle sysctlbyname = LINKER.downcallHandle(LIBC.findOrThrow("sysctlbyname"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, SIZE_T), CAPTURE_CALL_STATE);

    /**
     * Calls {@code sysctlbyname(name, oldp, oldlenp, newp, newlen)} with errno capture.
     *
     * @param callState segment allocated with {@link ForeignFunctions#CAPTURED_STATE_LAYOUT} for errno capture
     * @param name      MIB name segment (null-terminated UTF-8)
     * @param oldp      output buffer for the current value, or {@link MemorySegment#NULL} to query size only
     * @param oldlenp   pointer to a {@code size_t}: on input, the size of {@code oldp}; on output, the bytes written
     * @param newp      new value buffer, or {@link MemorySegment#NULL} when reading
     * @param newlen    size of {@code newp} in bytes, or 0 when reading
     * @return 0 on success, -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static int sysctlbyname(MemorySegment callState, MemorySegment name, MemorySegment oldp,
            MemorySegment oldlenp, MemorySegment newp, long newlen) throws Throwable {
        return (int) sysctlbyname.invokeExact(callState, name, oldp, oldlenp, newp, newlen);
    }

    // int getloadavg(double loadavg[], int nelem);
    private static final MethodHandle getloadavg = LINKER.downcallHandle(LIBC.findOrThrow("getloadavg"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /**
     * Calls {@code getloadavg(loadavg, nelem)}.
     *
     * @param loadavg pre-allocated segment of {@code nelem} doubles
     * @param nelem   number of load average values to retrieve (1–3)
     * @return number of samples set, or -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static int getloadavg(MemorySegment loadavg, int nelem) throws Throwable {
        return (int) getloadavg.invokeExact(loadavg, nelem);
    }
}
