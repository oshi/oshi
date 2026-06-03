/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.unix.freebsd;

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
 * FFM bindings for FreeBSD libc functions used by OSHI.
 * <p>
 * Phase 3 scope covers {@code sysctlbyname}, {@code getloadavg}, and the {@code utmpx} family
 * ({@code setutxent}/{@code getutxent}/{@code endutxent}) plus the matching {@link #UTMPX_LAYOUT} so a session driver
 * can walk the database. Additional libc bindings ({@code sysctl} with int MIB, {@code getpid}, {@code thr_self},
 * {@code getrlimit}) are added in later phases alongside their first FFM consumer.
 */
public final class FreeBsdLibcFunctions extends ForeignFunctions {

    private FreeBsdLibcFunctions() {
    }

    /** Layout of the C {@code size_t} type on FreeBSD (8 bytes on all supported 64-bit archs). */
    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    // ---- utmpx constants (FreeBSD <utmpx.h>) ----

    /** utmpx entry type: session leader of a logged in user. */
    public static final short LOGIN_PROCESS = 6;
    /** utmpx entry type: normal process. */
    public static final short USER_PROCESS = 7;

    static final int UTX_USERSIZE = 32;
    static final int UTX_LINESIZE = 16;
    static final int UTX_IDSIZE = 8;
    static final int UTX_HOSTSIZE = 128;
    static final int UTX_SPARESIZE = 64;

    /**
     * Layout of FreeBSD's {@code struct utmpx}.
     *
     * <pre>
     *   short            ut_type      (2)
     *   pad                           (6)
     *   long             ut_tv.tv_sec (8)
     *   long             ut_tv.tv_usec(8)
     *   char[8]          ut_id        (8)
     *   int              ut_pid       (4)
     *   char[32]         ut_user      (32)
     *   char[16]         ut_line      (16)
     *   char[128]        ut_host      (128)
     *   char[64]         __ut_spare   (64)
     *   total = 276 bytes
     * </pre>
     */
    public static final StructLayout UTMPX_LAYOUT = MemoryLayout.structLayout(JAVA_SHORT.withName("ut_type"),
            MemoryLayout.paddingLayout(6), JAVA_LONG.withName("ut_tv_sec"), JAVA_LONG.withName("ut_tv_usec"),
            MemoryLayout.sequenceLayout(UTX_IDSIZE, JAVA_BYTE).withName("ut_id"), JAVA_INT.withName("ut_pid"),
            MemoryLayout.sequenceLayout(UTX_USERSIZE, JAVA_BYTE).withName("ut_user"),
            MemoryLayout.sequenceLayout(UTX_LINESIZE, JAVA_BYTE).withName("ut_line"),
            MemoryLayout.sequenceLayout(UTX_HOSTSIZE, JAVA_BYTE).withName("ut_host"),
            MemoryLayout.sequenceLayout(UTX_SPARESIZE, JAVA_BYTE).withName("__ut_spare"));

    private static final VarHandle UTMPX_TYPE = UTMPX_LAYOUT.varHandle(PathElement.groupElement("ut_type"));
    private static final VarHandle UTMPX_TV_SEC = UTMPX_LAYOUT.varHandle(PathElement.groupElement("ut_tv_sec"));
    private static final VarHandle UTMPX_TV_USEC = UTMPX_LAYOUT.varHandle(PathElement.groupElement("ut_tv_usec"));

    private static final long UTMPX_USER_OFFSET = UTMPX_LAYOUT.byteOffset(PathElement.groupElement("ut_user"));
    private static final long UTMPX_LINE_OFFSET = UTMPX_LAYOUT.byteOffset(PathElement.groupElement("ut_line"));
    private static final long UTMPX_HOST_OFFSET = UTMPX_LAYOUT.byteOffset(PathElement.groupElement("ut_host"));

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
     * Reads {@code ut_user} from a utmpx segment as a null-terminated string.
     *
     * @param ut segment populated by {@link #getutxent()} and reinterpreted to {@link #UTMPX_LAYOUT}
     * @return the username string
     */
    public static String utmpxUser(MemorySegment ut) {
        return ut.asSlice(UTMPX_USER_OFFSET, UTX_USERSIZE).getString(0);
    }

    /**
     * Reads {@code ut_line} from a utmpx segment as a null-terminated string.
     *
     * @param ut segment populated by {@link #getutxent()} and reinterpreted to {@link #UTMPX_LAYOUT}
     * @return the device name string
     */
    public static String utmpxLine(MemorySegment ut) {
        return ut.asSlice(UTMPX_LINE_OFFSET, UTX_LINESIZE).getString(0);
    }

    /**
     * Reads {@code ut_host} from a utmpx segment as a null-terminated string.
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
