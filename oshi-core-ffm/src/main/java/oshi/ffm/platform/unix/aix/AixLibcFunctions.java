/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.unix.aix;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import oshi.ffm.platform.unix.PosixLibcFunctions;

/**
 * FFM bindings for AIX libc functions used by OSHI.
 * <p>
 * Most performance and configuration data on AIX comes from {@code libperfstat} (see {@link PerfstatFunctions}); the
 * libc surface here is limited to {@code thread_self} and the {@code open}/{@code close}/{@code pread} trio used to
 * read {@code /proc}. The POSIX bindings ({@code getpid}, {@code getrlimit}, {@code gethostname}) are inherited from
 * {@link PosixLibcFunctions}.
 */
public final class AixLibcFunctions extends PosixLibcFunctions {

    private AixLibcFunctions() {
    }

    /** Layout of the C {@code size_t} type on 64-bit AIX. */
    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    /** {@code getrlimit} resource: maximum number of open file descriptors. AIX value (7). */
    public static final int RLIMIT_NOFILE = 7;

    // tid_t thread_self(void); // AIX-specific. tid_t follows long -- measured at 4 bytes compiling
    // with -maix32 and 8 with -maix64 -- so it is 8 here, as SIZE_T above is, because FFM needs a
    // 64-bit JVM. Declaring it 4 read only the low half of the returned register. The JNA twin maps
    // it to NativeLong instead, which is the same width here and stays correct in either model.
    private static final MethodHandle thread_self = LINKER.downcallHandle(LIBC.findOrThrow("thread_self"),
            FunctionDescriptor.of(JAVA_LONG));

    /**
     * Calls {@code thread_self()} — returns the kernel thread ID of the calling thread (AIX).
     *
     * @return the kernel thread ID of the calling thread
     * @throws Throwable on FFM invocation error
     */
    public static long thread_self() throws Throwable {
        return (long) thread_self.invokeExact();
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

}
