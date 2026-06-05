/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.unix.solaris;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for Solaris/illumos libc functions used by OSHI.
 * <p>
 * Solaris uses kstat as its primary kernel-statistics interface, so the libc surface here is small: thread/process IDs
 * and {@code getrlimit}. The {@code RLIMIT_NOFILE} resource constant is {@code 5} on Solaris/illumos (not {@code 7} as
 * on Linux — see <a href="https://illumos.org/man/2/getrlimit">illumos getrlimit(2)</a>).
 */
public final class SolarisLibcFunctions extends ForeignFunctions {

    private SolarisLibcFunctions() {
    }

    /** Layout of the C {@code size_t} type on 64-bit Solaris/illumos. */
    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    /**
     * {@code getrlimit} resource: maximum number of open file descriptors. illumos/Solaris value (5). JNA's
     * {@code Resource.RLIMIT_NOFILE} is the Linux value (7), which on Solaris corresponds to {@code RLIM_NLIMITS}
     * (invalid); the JNA-side {@link oshi.jna.platform.unix.SolarisLibc#RLIMIT_NOFILE} shadows it with the correct
     * value.
     */
    public static final int RLIMIT_NOFILE = 5;

    /** Layout of Solaris/illumos {@code struct rlimit}: two {@code rlim_t} (LP64 long) fields. */
    public static final StructLayout RLIMIT_LAYOUT = MemoryLayout.structLayout(JAVA_LONG.withName("rlim_cur"),
            JAVA_LONG.withName("rlim_max"));

    private static final VarHandle RLIMIT_CUR = RLIMIT_LAYOUT.varHandle(PathElement.groupElement("rlim_cur"));
    private static final VarHandle RLIMIT_MAX = RLIMIT_LAYOUT.varHandle(PathElement.groupElement("rlim_max"));

    // libc is already loaded into the JVM process; defaultLookup() avoids versioning pitfalls.
    private static final SymbolLookup LIBC = LINKER.defaultLookup();

    // pid_t getpid(void);
    private static final MethodHandle getpid = LINKER.downcallHandle(LIBC.findOrThrow("getpid"),
            FunctionDescriptor.of(JAVA_INT));

    /**
     * Calls {@code getpid()}.
     *
     * @return the process ID of the calling process
     * @throws Throwable on FFM invocation error
     */
    public static int getpid() throws Throwable {
        return (int) getpid.invokeExact();
    }

    // thread_t thr_self(void); // Solaris-specific
    private static final MethodHandle thr_self = LINKER.downcallHandle(LIBC.findOrThrow("thr_self"),
            FunctionDescriptor.of(JAVA_INT));

    /**
     * Calls {@code thr_self()} — returns the thread ID of the calling thread (Solaris/illumos).
     *
     * @return the thread ID of the calling thread
     * @throws Throwable on FFM invocation error
     */
    public static int thr_self() throws Throwable {
        return (int) thr_self.invokeExact();
    }

    // int getrlimit(int resource, struct rlimit *rlim);
    private static final MethodHandle getrlimit = LINKER.downcallHandle(LIBC.findOrThrow("getrlimit"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));

    /**
     * Calls {@code getrlimit(resource, rlim)}.
     *
     * @param resource resource constant (e.g. {@link #RLIMIT_NOFILE})
     * @param rlim     segment allocated with {@link #RLIMIT_LAYOUT}
     * @return 0 on success, -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static int getrlimit(int resource, MemorySegment rlim) throws Throwable {
        return (int) getrlimit.invokeExact(resource, rlim);
    }

    /**
     * Reads {@code rlim_cur} from an rlimit segment populated by {@link #getrlimit(int, MemorySegment)}.
     *
     * @param rlim segment allocated with {@link #RLIMIT_LAYOUT}
     * @return the soft resource limit
     */
    public static long rlimitCur(MemorySegment rlim) {
        return (long) RLIMIT_CUR.get(rlim, 0L);
    }

    /**
     * Reads {@code rlim_max} from an rlimit segment populated by {@link #getrlimit(int, MemorySegment)}.
     *
     * @param rlim segment allocated with {@link #RLIMIT_LAYOUT}
     * @return the hard resource limit
     */
    public static long rlimitMax(MemorySegment rlim) {
        return (long) RLIMIT_MAX.get(rlim, 0L);
    }
}
