/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.unix.solaris;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import oshi.ffm.platform.unix.PosixLibcFunctions;

/**
 * FFM bindings for Solaris/illumos libc functions used by OSHI.
 * <p>
 * Solaris uses kstat as its primary kernel-statistics interface, so the libc surface here is small: {@code thr_self},
 * {@code getloadavg} and the {@code utmpx} family. The POSIX bindings ({@code getpid}, {@code getrlimit},
 * {@code gethostname}) are inherited from {@link PosixLibcFunctions}. The {@code RLIMIT_NOFILE} resource constant is
 * {@code 5} on Solaris/illumos (not {@code 7} as on Linux — see <a href="https://illumos.org/man/2/getrlimit">illumos
 * getrlimit(2)</a>), so it stays declared here.
 */
public final class SolarisLibcFunctions extends PosixLibcFunctions {

    private SolarisLibcFunctions() {
    }

    /** Layout of the C {@code size_t} type on 64-bit Solaris/illumos. */
    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    /**
     * {@code getrlimit} resource: maximum number of open file descriptors. illumos/Solaris value (5). JNA's
     * {@code Resource.RLIMIT_NOFILE} is the Linux value (7), which on Solaris corresponds to {@code RLIM_NLIMITS}
     * (invalid); the JNA-side {@code oshi.jna.platform.unix.SolarisLibc#RLIMIT_NOFILE} shadows it with the correct
     * value.
     */
    public static final int RLIMIT_NOFILE = 5;

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

    // ---- utmpx (Solaris/illumos <utmpx.h>) ----

    /** utmpx entry type: session leader of a logged in user. */
    public static final short LOGIN_PROCESS = 6;
    /** utmpx entry type: normal process. */
    public static final short USER_PROCESS = 7;

    static final int UTX_USERSIZE = 32;
    static final int UTX_IDSIZE = 4;
    static final int UTX_LINESIZE = 32;
    static final int UTX_HOSTSIZE = 257;

    /**
     * Layout of Solaris/illumos {@code struct utmpx} on LP64. Mirrors the JNA {@code SolarisLibc.SolarisUtmpx} struct.
     *
     * <pre>
     *   char[32]  ut_user                      (32) @ 0
     *   char[4]   ut_id                         (4) @ 32
     *   char[32]  ut_line                      (32) @ 36
     *   int       ut_pid                        (4) @ 68
     *   short     ut_type                       (2) @ 72
     *   short[2]  ut_exit {e_termination,e_exit}(4) @ 74
     *   pad (align ut_tv to 8)                  (2) @ 78
     *   long      ut_tv.tv_sec                   (8) @ 80
     *   long      ut_tv.tv_usec                  (8) @ 88
     *   int       ut_session                     (4) @ 96
     *   int[5]    pad                           (20) @ 100
     *   short     ut_syslen                      (2) @ 120
     *   char[257] ut_host                      (257) @ 122
     *   trailing pad (align struct to 8)         (5) @ 379
     *   total = 384 bytes
     * </pre>
     */
    public static final StructLayout UTMPX_LAYOUT = MemoryLayout.structLayout(
            MemoryLayout.sequenceLayout(UTX_USERSIZE, JAVA_BYTE).withName("ut_user"),
            MemoryLayout.sequenceLayout(UTX_IDSIZE, JAVA_BYTE).withName("ut_id"),
            MemoryLayout.sequenceLayout(UTX_LINESIZE, JAVA_BYTE).withName("ut_line"), JAVA_INT.withName("ut_pid"),
            JAVA_SHORT.withName("ut_type"), MemoryLayout.paddingLayout(6),
            // nested rather than flattened so the audit can check ut_tv's offset against the header
            MemoryLayout.structLayout(JAVA_LONG.withName("tv_sec"), JAVA_LONG.withName("tv_usec")).withName("ut_tv"),
            MemoryLayout.paddingLayout(26), MemoryLayout.sequenceLayout(UTX_HOSTSIZE, JAVA_BYTE).withName("ut_host"),
            MemoryLayout.paddingLayout(5));

    private static final VarHandle UTMPX_TYPE = UTMPX_LAYOUT.varHandle(PathElement.groupElement("ut_type"));
    private static final VarHandle UTMPX_TV_SEC = UTMPX_LAYOUT.varHandle(PathElement.groupElement("ut_tv"),
            PathElement.groupElement("tv_sec"));
    private static final VarHandle UTMPX_TV_USEC = UTMPX_LAYOUT.varHandle(PathElement.groupElement("ut_tv"),
            PathElement.groupElement("tv_usec"));

    private static final long UTMPX_USER_OFFSET = UTMPX_LAYOUT.byteOffset(PathElement.groupElement("ut_user"));
    private static final long UTMPX_LINE_OFFSET = UTMPX_LAYOUT.byteOffset(PathElement.groupElement("ut_line"));
    private static final long UTMPX_HOST_OFFSET = UTMPX_LAYOUT.byteOffset(PathElement.groupElement("ut_host"));

    // void setutxent(void);
    private static final MethodHandle setutxent = LINKER.downcallHandle(LIBC.findOrThrow("setutxent"),
            FunctionDescriptor.ofVoid());

    /** Rewinds the utmpx database. Not thread safe — call sites must synchronize externally. */
    public static void setutxent() throws Throwable {
        setutxent.invokeExact();
    }

    // struct utmpx * getutxent(void);
    private static final MethodHandle getutxent = LINKER.downcallHandle(LIBC.findOrThrow("getutxent"),
            FunctionDescriptor.of(ADDRESS));

    /**
     * Reads the next entry from the utmpx database.
     *
     * @return a pointer to the utmpx structure, or {@code null} if no more entries
     * @throws Throwable on FFM invocation error
     */
    public static MemorySegment getutxent() throws Throwable {
        MemorySegment result = (MemorySegment) getutxent.invokeExact();
        return result.equals(MemorySegment.NULL) ? null : result;
    }

    // void endutxent(void);
    private static final MethodHandle endutxent = LINKER.downcallHandle(LIBC.findOrThrow("endutxent"),
            FunctionDescriptor.ofVoid());

    /** Closes the utmpx database. */
    public static void endutxent() throws Throwable {
        endutxent.invokeExact();
    }

    /**
     * Reads {@code ut_type} from a utmpx segment.
     *
     * @param ut segment populated by {@link #getutxent()} and reinterpreted to {@link #UTMPX_LAYOUT}
     * @return the entry type
     */
    public static short utmpxType(MemorySegment ut) {
        return (short) UTMPX_TYPE.get(ut, 0L);
    }

    /**
     * Reads {@code ut_user} from a utmpx segment as a null-terminated UTF-8 string.
     *
     * @param ut segment populated by {@link #getutxent()} and reinterpreted to {@link #UTMPX_LAYOUT}
     * @return the username string
     */
    public static String utmpxUser(MemorySegment ut) {
        return readFixedWidthString(ut, UTMPX_USER_OFFSET, UTX_USERSIZE);
    }

    /**
     * Reads {@code ut_line} from a utmpx segment as a null-terminated UTF-8 string.
     *
     * @param ut segment populated by {@link #getutxent()} and reinterpreted to {@link #UTMPX_LAYOUT}
     * @return the device name string
     */
    public static String utmpxLine(MemorySegment ut) {
        return readFixedWidthString(ut, UTMPX_LINE_OFFSET, UTX_LINESIZE);
    }

    /**
     * Reads {@code ut_host} from a utmpx segment as a null-terminated UTF-8 string.
     *
     * @param ut segment populated by {@link #getutxent()} and reinterpreted to {@link #UTMPX_LAYOUT}
     * @return the host name string
     */
    public static String utmpxHost(MemorySegment ut) {
        return readFixedWidthString(ut, UTMPX_HOST_OFFSET, UTX_HOSTSIZE);
    }

    /**
     * Reads the login time from a utmpx segment as epoch milliseconds.
     *
     * @param ut segment populated by {@link #getutxent()} and reinterpreted to {@link #UTMPX_LAYOUT}
     * @return login time in milliseconds since epoch
     */
    public static long utmpxLoginTime(MemorySegment ut) {
        long tvSec = (long) UTMPX_TV_SEC.get(ut, 0L);
        long tvUsec = (long) UTMPX_TV_USEC.get(ut, 0L);
        return tvSec * 1000L + tvUsec / 1000L;
    }
}
