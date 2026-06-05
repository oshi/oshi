/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.unix.openbsd;

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
 * FFM bindings for OpenBSD libc functions used by OSHI.
 * <p>
 * OpenBSD uses {@code sysctl(int *name, u_int namelen, ...)} exclusively (no {@code sysctlbyname}). Additional bindings
 * ({@code getthrid}, {@code getrlimit}) support process and resource limit queries.
 */
public final class OpenBsdLibcFunctions extends ForeignFunctions {

    private OpenBsdLibcFunctions() {
    }

    /** Layout of the C {@code size_t} type on OpenBSD (8 bytes on all supported 64-bit archs). */
    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    /** {@code getrlimit} resource: maximum number of open file descriptors. OpenBSD value (8). */
    public static final int RLIMIT_NOFILE = 8;

    /**
     * Layout of OpenBSD's {@code struct rlimit}: two {@code rlim_t} (LP64 long) fields.
     */
    public static final StructLayout RLIMIT_LAYOUT = MemoryLayout.structLayout(JAVA_LONG.withName("rlim_cur"),
            JAVA_LONG.withName("rlim_max"));

    private static final VarHandle RLIMIT_CUR = RLIMIT_LAYOUT.varHandle(PathElement.groupElement("rlim_cur"));
    private static final VarHandle RLIMIT_MAX = RLIMIT_LAYOUT.varHandle(PathElement.groupElement("rlim_max"));

    // libc is already loaded into the JVM process; defaultLookup() avoids versioning pitfalls.
    private static final SymbolLookup LIBC = LINKER.defaultLookup();

    // int sysctl(int *name, u_int namelen, void *oldp, size_t *oldlenp, void *newp, size_t newlen);
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

    // int getthrid(void);
    private static final MethodHandle getthrid = LINKER.downcallHandle(LIBC.findOrThrow("getthrid"),
            FunctionDescriptor.of(JAVA_INT));

    /**
     * Calls {@code getthrid()} — returns the thread ID of the calling thread.
     *
     * @return the thread ID of the calling thread
     * @throws Throwable on FFM invocation error
     */
    public static int getthrid() throws Throwable {
        return (int) getthrid.invokeExact();
    }

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

    // --- Sysctl MIB constants ---

    public static final int CTL_KERN = 1;
    public static final int CTL_HW = 6;
    public static final int CTL_VFS = 10;

    public static final int KERN_OSTYPE = 1;
    public static final int KERN_OSRELEASE = 2;
    public static final int KERN_VERSION = 4;
    public static final int KERN_ARGMAX = 8;
    public static final int KERN_CPTIME = 40;
    public static final int KERN_CPTIME2 = 71;

    public static final int HW_MACHINE = 1;
    public static final int HW_MODEL = 2;
    public static final int HW_PAGESIZE = 7;
    public static final int HW_CPUSPEED = 12;
    public static final int HW_NCPUFOUND = 21;
    public static final int HW_NCPUONLINE = 25;

    public static final int VFS_GENERIC = 0;
    public static final int VFS_BCACHESTAT = 3;

    public static final int CPUSTATES = 5;
    public static final int CP_USER = 0;
    public static final int CP_NICE = 1;
    public static final int CP_SYS = 2;
    public static final int CP_INTR = 3;
    public static final int CP_IDLE = 4;

    public static final int KERN_PROC_ARGS = 55;
    public static final int KERN_PROC_ARGV = 1;
    public static final int KERN_PROC_ENV = 3;

    // --- Bcachestats struct layout (18 int64_t fields) ---

    public static final StructLayout BCACHESTATS_LAYOUT = MemoryLayout.structLayout(JAVA_LONG.withName("numbufs"),
            JAVA_LONG.withName("numbufpages"), JAVA_LONG.withName("numdirtypages"), JAVA_LONG.withName("numcleanpages"),
            JAVA_LONG.withName("pendingwrites"), JAVA_LONG.withName("pendingreads"), JAVA_LONG.withName("numwrites"),
            JAVA_LONG.withName("numreads"), JAVA_LONG.withName("cachehits"), JAVA_LONG.withName("busymapped"),
            JAVA_LONG.withName("dmapages"), JAVA_LONG.withName("highpages"), JAVA_LONG.withName("delwribufs"),
            JAVA_LONG.withName("kvaslots"), JAVA_LONG.withName("kvaslots_avail"), JAVA_LONG.withName("highflips"),
            JAVA_LONG.withName("highflops"), JAVA_LONG.withName("dmaflips"));

    private static final VarHandle BCACHESTATS_NUMBUFPAGES = BCACHESTATS_LAYOUT
            .varHandle(PathElement.groupElement("numbufpages"));

    /**
     * Reads the {@code numbufpages} field from a Bcachestats segment.
     *
     * @param seg segment allocated with {@link #BCACHESTATS_LAYOUT}
     * @return the number of pages in the buffer cache
     */
    public static long bcachestatsNumbufpages(MemorySegment seg) {
        return (long) BCACHESTATS_NUMBUFPAGES.get(seg, 0L);
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
}
