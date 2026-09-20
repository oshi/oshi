/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.unix.netbsd;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.PosixLibcFunctions;

/**
 * FFM bindings for NetBSD libc functions used by OSHI.
 * <p>
 * Binds {@code sysctl}, {@code _lwp_self} and {@code getloadavg}; the POSIX bindings ({@code getpid},
 * {@code getrlimit}) are inherited from {@link PosixLibcFunctions}.
 */
public final class NetBsdLibcFunctions extends PosixLibcFunctions {

    private NetBsdLibcFunctions() {
    }

    /** Layout of the C {@code size_t} type on NetBSD (8 bytes on all supported 64-bit archs). */
    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    /** {@code getrlimit} resource: maximum number of open file descriptors. NetBSD value (8), not Linux's 7. */
    public static final int RLIMIT_NOFILE = 8;

    // int sysctl(const int *name, u_int namelen, void *oldp, size_t *oldlenp, const void *newp, size_t newlen);
    private static final MethodHandle sysctl = LINKER.downcallHandle(LIBC.findOrThrow("sysctl"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, SIZE_T), CAPTURE_CALL_STATE);

    /**
     * Calls {@code sysctl(name, namelen, oldp, oldlenp, newp, newlen)} with errno capture.
     *
     * @param callState segment allocated with {@link ForeignFunctions#CAPTURED_STATE_LAYOUT} for errno capture
     * @param name      MIB array segment (sequence of ints)
     * @param namelen   number of ints in {@code name}
     * @param oldp      output buffer for the current value, or {@link MemorySegment#NULL} to query size only
     * @param oldlenp   pointer to a {@code size_t}: on input, the size of {@code oldp}; on output, the bytes written
     * @param newp      new value buffer, or {@link MemorySegment#NULL} when reading
     * @param newlen    size of {@code newp} in bytes, or 0 when reading
     * @return 0 on success, -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static int sysctl(MemorySegment callState, MemorySegment name, int namelen, MemorySegment oldp,
            MemorySegment oldlenp, MemorySegment newp, long newlen) throws Throwable {
        return (int) sysctl.invokeExact(callState, name, namelen, oldp, oldlenp, newp, newlen);
    }

    // lwpid_t _lwp_self(void);
    private static final MethodHandle lwpSelf = LINKER.downcallHandle(LIBC.findOrThrow("_lwp_self"),
            FunctionDescriptor.of(JAVA_INT));

    /**
     * Calls {@code _lwp_self()} — returns the LWP (lightweight process/thread) ID of the calling thread.
     *
     * @return the LWP ID of the calling thread
     * @throws Throwable on FFM invocation error
     */
    public static int lwpSelf() throws Throwable {
        return (int) lwpSelf.invokeExact();
    }

    // int getloadavg(double loadavg[], int nelem);
    private static final MethodHandle getloadavg = LINKER.downcallHandle(LIBC.findOrThrow("getloadavg"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /**
     * Calls {@code getloadavg(loadavg, nelem)}.
     *
     * @param loadavg pre-allocated segment of {@code nelem} doubles
     * @param nelem   number of load average values to retrieve (1-3)
     * @return number of samples set, or -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static int getloadavg(MemorySegment loadavg, int nelem) throws Throwable {
        return (int) getloadavg.invokeExact(loadavg, nelem);
    }

    // --- Sysctl MIB constants, from <sys/sysctl.h> ---

    public static final int CTL_KERN = 1;

    public static final int KERN_ARGMAX = 8;
    public static final int KERN_CP_TIME = 51;
    public static final int KERN_PROC_ARGS = 48;
    public static final int KERN_PROC_ENV = 3;
}
