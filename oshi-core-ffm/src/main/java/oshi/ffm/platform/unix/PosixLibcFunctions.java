/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.unix;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for the libc functions POSIX requires every UNIX to provide, shared by the per-platform
 * {@code *LibcFunctions} classes.
 * <p>
 * <strong>Membership rule: POSIX-mandated functions only.</strong> These handles are resolved with {@code findOrThrow}
 * in a static initializer, so a symbol missing on any one platform would raise {@code ExceptionInInitializerError}
 * there and take every other member of this class down with it. Being POSIX-mandated is what guarantees the symbol
 * exists everywhere, so it is the criterion for belonging here rather than a stylistic preference.
 * <p>
 * {@code getloadavg} is the cautionary example: it looks like a peer of these, and four of the six platforms bind it,
 * but it is a BSD extension rather than POSIX and AIX does not have it (AIX reads load average from
 * {@code perfstat_cpu_total} instead). It therefore stays in the per-platform classes.
 * <p>
 * Values that POSIX names but does not fix stay per-platform too: {@code RLIMIT_NOFILE} is 7 on Linux and AIX, 8 on the
 * BSDs, and 5 on Solaris/illumos, so each subclass declares its own.
 */
public abstract class PosixLibcFunctions extends ForeignFunctions {

    /**
     * Constructs a new {@code PosixLibcFunctions}. Subclasses are utility holders and are never instantiated; this
     * exists only so they can inherit the members below.
     */
    protected PosixLibcFunctions() {
    }

    // libc is already loaded into the JVM process, so defaultLookup() finds it without the libc.so versioning
    // pitfalls that SymbolLookup.libraryLookup("c") hits on some platforms.
    private static final SymbolLookup LIBC = LINKER.defaultLookup();

    /** Layout of {@code struct rlimit}: two {@code rlim_t} (LP64 long) fields. */
    public static final StructLayout RLIMIT_LAYOUT = MemoryLayout.structLayout(JAVA_LONG.withName("rlim_cur"),
            JAVA_LONG.withName("rlim_max"));

    private static final VarHandle RLIMIT_CUR = RLIMIT_LAYOUT.varHandle(PathElement.groupElement("rlim_cur"));
    private static final VarHandle RLIMIT_MAX = RLIMIT_LAYOUT.varHandle(PathElement.groupElement("rlim_max"));

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
     * @param resource resource constant; use the calling class's {@code RLIMIT_NOFILE}, whose value is
     *                 platform-specific
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

    // int gethostname(char *name, size_t namelen);
    private static final MethodHandle gethostname = LINKER.downcallHandle(LIBC.findOrThrow("gethostname"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG));

    /**
     * Calls {@code gethostname(name, namelen)}.
     *
     * @param name    buffer to receive the host name
     * @param namelen size of the buffer in bytes
     * @return 0 on success, -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static int gethostname(MemorySegment name, long namelen) throws Throwable {
        return (int) gethostname.invokeExact(name, namelen);
    }
}
