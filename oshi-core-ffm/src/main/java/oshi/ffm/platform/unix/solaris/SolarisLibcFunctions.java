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
     * (invalid); the JNA-side {@code oshi.jna.platform.unix.SolarisLibc#RLIMIT_NOFILE} shadows it with the correct
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
            JAVA_SHORT.withName("ut_type"), MemoryLayout.paddingLayout(6), JAVA_LONG.withName("ut_tv_sec"),
            JAVA_LONG.withName("ut_tv_usec"), MemoryLayout.paddingLayout(26),
            MemoryLayout.sequenceLayout(UTX_HOSTSIZE, JAVA_BYTE).withName("ut_host"), MemoryLayout.paddingLayout(5));

    private static final VarHandle UTMPX_TYPE = UTMPX_LAYOUT.varHandle(PathElement.groupElement("ut_type"));
    private static final VarHandle UTMPX_TV_SEC = UTMPX_LAYOUT.varHandle(PathElement.groupElement("ut_tv_sec"));
    private static final VarHandle UTMPX_TV_USEC = UTMPX_LAYOUT.varHandle(PathElement.groupElement("ut_tv_usec"));

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
        return ut.asSlice(UTMPX_USER_OFFSET, UTX_USERSIZE).getString(0);
    }

    /**
     * Reads {@code ut_line} from a utmpx segment as a null-terminated UTF-8 string.
     *
     * @param ut segment populated by {@link #getutxent()} and reinterpreted to {@link #UTMPX_LAYOUT}
     * @return the device name string
     */
    public static String utmpxLine(MemorySegment ut) {
        return ut.asSlice(UTMPX_LINE_OFFSET, UTX_LINESIZE).getString(0);
    }

    /**
     * Reads {@code ut_host} from a utmpx segment as a null-terminated UTF-8 string.
     *
     * @param ut segment populated by {@link #getutxent()} and reinterpreted to {@link #UTMPX_LAYOUT}
     * @return the host name string
     */
    public static String utmpxHost(MemorySegment ut) {
        return ut.asSlice(UTMPX_HOST_OFFSET, UTX_HOSTSIZE).getString(0);
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
