/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.unix.aix;

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
 * FFM bindings for AIX libc functions used by OSHI.
 * <p>
 * Most performance and configuration data on AIX comes from {@code libperfstat} (see {@link PerfstatFunctions}); the
 * libc surface here is limited to thread/process IDs, hostname, and {@code getrlimit}.
 */
public final class AixLibcFunctions extends ForeignFunctions {

    private AixLibcFunctions() {
    }

    /** Layout of the C {@code size_t} type on 64-bit AIX. */
    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    /** {@code getrlimit} resource: maximum number of open file descriptors. AIX value (7). */
    public static final int RLIMIT_NOFILE = 7;

    /** Layout of AIX {@code struct rlimit}: two {@code rlim_t} (LP64 long) fields. */
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

    // tid_t thread_self(void); // AIX-specific
    private static final MethodHandle thread_self = LINKER.downcallHandle(LIBC.findOrThrow("thread_self"),
            FunctionDescriptor.of(JAVA_INT));

    /**
     * Calls {@code thread_self()} — returns the kernel thread ID of the calling thread (AIX).
     *
     * @return the kernel thread ID of the calling thread
     * @throws Throwable on FFM invocation error
     */
    public static int thread_self() throws Throwable {
        return (int) thread_self.invokeExact();
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

    // int open(const char *path, int flags);
    private static final MethodHandle open = LINKER.downcallHandle(LIBC.findOrThrow("open"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /**
     * Calls {@code open(path, flags)}.
     *
     * @param path  null-terminated path segment
     * @param flags open flags (e.g. {@code O_RDONLY})
     * @return file descriptor on success, -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static int open(MemorySegment path, int flags) throws Throwable {
        return (int) open.invokeExact(path, flags);
    }

    // int close(int fd);
    private static final MethodHandle close = LINKER.downcallHandle(LIBC.findOrThrow("close"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    /**
     * Calls {@code close(fd)}.
     *
     * @param fd file descriptor
     * @return 0 on success, -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static int close(int fd) throws Throwable {
        return (int) close.invokeExact(fd);
    }

    // ssize_t pread(int fd, void *buf, size_t count, off_t offset);
    private static final MethodHandle pread = LINKER.downcallHandle(LIBC.findOrThrow("pread"),
            FunctionDescriptor.of(SIZE_T, JAVA_INT, ADDRESS, SIZE_T, JAVA_LONG));

    /**
     * Calls {@code pread(fd, buf, count, offset)}. {@code ssize_t} is mapped to {@code long}.
     *
     * @param fd     file descriptor
     * @param buf    buffer for the read
     * @param count  number of bytes to read
     * @param offset starting byte offset in the file
     * @return number of bytes actually read, or -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static long pread(int fd, MemorySegment buf, long count, long offset) throws Throwable {
        return (long) pread.invokeExact(fd, buf, count, offset);
    }

    // int gethostname(char *name, size_t namelen);
    private static final MethodHandle gethostname = LINKER.downcallHandle(LIBC.findOrThrow("gethostname"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, SIZE_T));

    /**
     * Calls {@code gethostname(name, namelen)}.
     *
     * @param name    buffer for the hostname (allocated by caller)
     * @param namelen size of {@code name} in bytes
     * @return 0 on success, -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static int gethostname(MemorySegment name, long namelen) throws Throwable {
        return (int) gethostname.invokeExact(name, namelen);
    }
}
