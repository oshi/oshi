/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.linux;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for Linux libc functions used by OSHI.
 * <p>
 * Covers: {@code getpid}, {@code gettid}, {@code syscall}, {@code getloadavg}, {@code sysinfo}, {@code statvfs},
 * {@code gethostname}, {@code getaddrinfo}/{@code freeaddrinfo}/{@code gai_strerror}, and {@code getrlimit}.
 */
public final class LinuxLibcFunctions extends ForeignFunctions {

    private LinuxLibcFunctions() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(LinuxLibcFunctions.class);

    // ---- SYS_GETTID syscall numbers ----
    // x86-64=186, x86-32=224, ARM64=178
    private static final long SYS_GETTID;

    static {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64") || arch.contains("riscv64")
                || arch.contains("loongarch64")) {
            SYS_GETTID = 178L;
        } else if (arch.contains("amd64") || arch.contains("x86_64")) {
            SYS_GETTID = 186L;
        } else if (arch.contains("s390")) {
            SYS_GETTID = 236L;
        } else if (arch.contains("ppc64")) {
            SYS_GETTID = 207L;
        } else {
            // x86-32 and ARM32 use 224; log for any other unexpected arch
            LOG.debug("Unknown architecture '{}' for SYS_GETTID, defaulting to 224", arch);
            SYS_GETTID = 224L;
        }
    }

    // ---- RLIMIT_NOFILE ----
    public static final int RLIMIT_NOFILE = 7;

    // ---- AI_CANONNAME ----
    public static final int AI_CANONNAME = 2;

    // ---- Struct layouts (64-bit Linux) ----

    /**
     * {@code struct sysinfo} layout (64-bit Linux).
     *
     * <pre>
     *   long    uptime       (8)
     *   long[3] loads        (24)
     *   long    totalram     (8)
     *   long    freeram      (8)
     *   long    sharedram    (8)
     *   long    bufferram    (8)
     *   long    totalswap    (8)
     *   long    freeswap     (8)
     *   short   procs        (2)
     *   ... padding/high memory fields follow
     * </pre>
     */
    public static final StructLayout SYSINFO_LAYOUT = MemoryLayout.structLayout(JAVA_LONG.withName("uptime"),
            MemoryLayout.sequenceLayout(3, JAVA_LONG).withName("loads"), JAVA_LONG.withName("totalram"),
            JAVA_LONG.withName("freeram"), JAVA_LONG.withName("sharedram"), JAVA_LONG.withName("bufferram"),
            JAVA_LONG.withName("totalswap"), JAVA_LONG.withName("freeswap"), JAVA_SHORT.withName("procs"),
            MemoryLayout.paddingLayout(6), // pad to 8-byte alignment before totalhigh
            JAVA_LONG.withName("totalhigh"), JAVA_LONG.withName("freehigh"), JAVA_INT.withName("mem_unit"),
            MemoryLayout.paddingLayout(4) // pad struct to 112 bytes total
    );

    private static final VarHandle SYSINFO_PROCS = SYSINFO_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("procs"));

    /**
     * {@code struct statvfs} layout (64-bit Linux, no {@code _f_unused} padding field).
     */
    public static final StructLayout STATVFS_LAYOUT = MemoryLayout.structLayout(JAVA_LONG.withName("f_bsize"),
            JAVA_LONG.withName("f_frsize"), JAVA_LONG.withName("f_blocks"), JAVA_LONG.withName("f_bfree"),
            JAVA_LONG.withName("f_bavail"), JAVA_LONG.withName("f_files"), JAVA_LONG.withName("f_ffree"),
            JAVA_LONG.withName("f_favail"), JAVA_LONG.withName("f_fsid"), JAVA_LONG.withName("f_flag"),
            JAVA_LONG.withName("f_namemax"), MemoryLayout.sequenceLayout(6, JAVA_INT).withName("_f_spare"));

    private static final VarHandle STATVFS_F_FRSIZE = STATVFS_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("f_frsize"));
    private static final VarHandle STATVFS_F_BLOCKS = STATVFS_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("f_blocks"));
    private static final VarHandle STATVFS_F_BFREE = STATVFS_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("f_bfree"));
    private static final VarHandle STATVFS_F_BAVAIL = STATVFS_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("f_bavail"));
    private static final VarHandle STATVFS_F_FILES = STATVFS_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("f_files"));
    private static final VarHandle STATVFS_F_FFREE = STATVFS_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("f_ffree"));

    /**
     * {@code struct rlimit} layout: two {@code unsigned long} fields.
     */
    public static final StructLayout RLIMIT_LAYOUT = MemoryLayout.structLayout(JAVA_LONG.withName("rlim_cur"),
            JAVA_LONG.withName("rlim_max"));

    private static final VarHandle RLIMIT_CUR = RLIMIT_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("rlim_cur"));
    private static final VarHandle RLIMIT_MAX = RLIMIT_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("rlim_max"));

    /**
     * {@code struct addrinfo} layout (64-bit Linux).
     *
     * <pre>
     *   int     ai_flags     (4)
     *   int     ai_family    (4)
     *   int     ai_socktype  (4)
     *   int     ai_protocol  (4)
     *   long    ai_addrlen   (8)   -- socklen_t padded to 8
     *   ptr     ai_addr      (8)
     *   ptr     ai_canonname (8)   -- offset 32
     *   ptr     ai_next      (8)
     * </pre>
     */
    public static final StructLayout ADDRINFO_LAYOUT = MemoryLayout.structLayout(JAVA_INT.withName("ai_flags"),
            JAVA_INT.withName("ai_family"), JAVA_INT.withName("ai_socktype"), JAVA_INT.withName("ai_protocol"),
            JAVA_LONG.withName("ai_addrlen"), ADDRESS.withName("ai_addr"), ADDRESS.withName("ai_canonname"),
            ADDRESS.withName("ai_next"));

    private static final VarHandle ADDRINFO_CANONNAME = ADDRINFO_LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("ai_canonname"));

    // ---- Method handles ----

    private static final MethodHandle getpid;
    private static final MethodHandle gettid;
    private static final MethodHandle syscall;
    private static final MethodHandle getloadavg;
    private static final MethodHandle sysinfo;
    private static final MethodHandle statvfs;
    private static final MethodHandle gethostname;
    private static final MethodHandle getaddrinfo;
    private static final MethodHandle freeaddrinfo;
    private static final MethodHandle gai_strerror;
    private static final MethodHandle getrlimit;

    private static final boolean HAS_GETTID;

    static {
        // libc is always linked into the JVM process; use the linker's default lookup
        // rather than libraryLookup("c") which fails on Linux (libc.so vs libc.so.6).
        SymbolLookup libc = LINKER.defaultLookup();

        getpid = LINKER.downcallHandle(libc.findOrThrow("getpid"), FunctionDescriptor.of(JAVA_INT));
        // syscall(SYS_GETTID) — declare with one fixed arg; no extra args needed for gettid
        syscall = LINKER.downcallHandle(libc.findOrThrow("syscall"), FunctionDescriptor.of(JAVA_LONG, JAVA_LONG),
                Linker.Option.firstVariadicArg(1));
        getloadavg = LINKER.downcallHandle(libc.findOrThrow("getloadavg"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        sysinfo = LINKER.downcallHandle(libc.findOrThrow("sysinfo"), FunctionDescriptor.of(JAVA_INT, ADDRESS));
        statvfs = LINKER.downcallHandle(libc.findOrThrow("statvfs"), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        gethostname = LINKER.downcallHandle(libc.findOrThrow("gethostname"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG));
        getaddrinfo = LINKER.downcallHandle(libc.findOrThrow("getaddrinfo"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        freeaddrinfo = LINKER.downcallHandle(libc.findOrThrow("freeaddrinfo"), FunctionDescriptor.ofVoid(ADDRESS));
        gai_strerror = LINKER.downcallHandle(libc.findOrThrow("gai_strerror"),
                FunctionDescriptor.of(ADDRESS, JAVA_INT));
        getrlimit = LINKER.downcallHandle(libc.findOrThrow("getrlimit"),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));

        MethodHandle hGettid = null;
        boolean hasGettid = false;
        try {
            hGettid = LINKER.downcallHandle(libc.findOrThrow("gettid"), FunctionDescriptor.of(JAVA_INT));
            hasGettid = true;
        } catch (Throwable e) {
            LOG.debug("gettid not found in libc, will use syscall fallback. {}", e.toString());
        }
        gettid = hGettid;
        HAS_GETTID = hasGettid;
    }

    /**
     * Returns whether {@code gettid()} is directly available in libc.
     *
     * @return {@code true} if {@code gettid} is available
     */
    public static boolean hasGettid() {
        return HAS_GETTID;
    }

    /**
     * Calls {@code getpid()}.
     *
     * @return the process ID of the calling process
     * @throws Throwable if the native call fails
     */
    public static int getpid() throws Throwable {
        return (int) getpid.invokeExact();
    }

    /**
     * Calls {@code gettid()} directly. Only valid if {@link #hasGettid()} is true.
     *
     * @return the thread ID of the calling thread
     * @throws Throwable if the native call fails
     */
    public static int gettid() throws Throwable {
        return (int) gettid.invokeExact();
    }

    /**
     * Calls {@code syscall(SYS_GETTID)}.
     *
     * @return the thread ID of the calling thread
     * @throws Throwable if the native call fails
     */
    public static long syscallGettid() throws Throwable {
        return (long) syscall.invokeExact(SYS_GETTID);
    }

    /**
     * Calls {@code getloadavg(double[] loadavg, int nelem)}.
     *
     * @param loadavg pre-allocated segment of {@code nelem} doubles
     * @param nelem   number of load average values to retrieve (1–3)
     * @return number of samples set, or -1 on error
     */
    public static int getloadavg(MemorySegment loadavg, int nelem) throws Throwable {
        return (int) getloadavg.invokeExact(loadavg, nelem);
    }

    /**
     * Calls {@code sysinfo(struct sysinfo *info)}.
     *
     * @param info segment allocated with {@link #SYSINFO_LAYOUT}
     * @return 0 on success, -1 on error
     */
    public static int sysinfo(MemorySegment info) throws Throwable {
        return (int) sysinfo.invokeExact(info);
    }

    /**
     * Reads the {@code procs} field from a sysinfo segment.
     *
     * @param info segment populated by {@link #sysinfo(MemorySegment)}
     * @return number of current processes
     */
    public static int sysinfoProcs(MemorySegment info) {
        return Short.toUnsignedInt((short) SYSINFO_PROCS.get(info, 0L));
    }

    /**
     * Calls {@code statvfs(const char *path, struct statvfs *buf)}.
     *
     * @param path path segment (null-terminated UTF-8)
     * @param buf  segment allocated with {@link #STATVFS_LAYOUT}
     * @return 0 on success, -1 on error
     */
    public static int statvfs(MemorySegment path, MemorySegment buf) throws Throwable {
        return (int) statvfs.invokeExact(path, buf);
    }

    /**
     * Reads {@code f_frsize} from a statvfs segment.
     *
     * @param buf segment populated by {@link #statvfs(MemorySegment, MemorySegment)}
     * @return fragment size in bytes
     */
    public static long statvfsFrsize(MemorySegment buf) {
        return (long) STATVFS_F_FRSIZE.get(buf, 0L);
    }

    /**
     * Reads {@code f_blocks} from a statvfs segment.
     *
     * @param buf segment populated by {@link #statvfs(MemorySegment, MemorySegment)}
     * @return total data blocks in filesystem
     */
    public static long statvfsBlocks(MemorySegment buf) {
        return (long) STATVFS_F_BLOCKS.get(buf, 0L);
    }

    /**
     * Reads {@code f_bfree} from a statvfs segment.
     *
     * @param buf segment populated by {@link #statvfs(MemorySegment, MemorySegment)}
     * @return free blocks in filesystem
     */
    public static long statvfsBfree(MemorySegment buf) {
        return (long) STATVFS_F_BFREE.get(buf, 0L);
    }

    /**
     * Reads {@code f_bavail} from a statvfs segment.
     *
     * @param buf segment populated by {@link #statvfs(MemorySegment, MemorySegment)}
     * @return free blocks available to unprivileged users
     */
    public static long statvfsBavail(MemorySegment buf) {
        return (long) STATVFS_F_BAVAIL.get(buf, 0L);
    }

    /**
     * Reads {@code f_files} from a statvfs segment.
     *
     * @param buf segment populated by {@link #statvfs(MemorySegment, MemorySegment)}
     * @return total file nodes in filesystem
     */
    public static long statvfsFiles(MemorySegment buf) {
        return (long) STATVFS_F_FILES.get(buf, 0L);
    }

    /**
     * Reads {@code f_ffree} from a statvfs segment.
     *
     * @param buf segment populated by {@link #statvfs(MemorySegment, MemorySegment)}
     * @return free file nodes in filesystem
     */
    public static long statvfsFfree(MemorySegment buf) {
        return (long) STATVFS_F_FFREE.get(buf, 0L);
    }

    /**
     * Calls {@code gethostname(char *name, size_t len)}.
     *
     * @param buf segment of at least {@code len} bytes
     * @param len buffer length
     * @return 0 on success, -1 on error
     */
    public static int gethostname(MemorySegment buf, long len) throws Throwable {
        return (int) gethostname.invokeExact(buf, len);
    }

    /**
     * Calls {@code getaddrinfo(node, service, hints, res)}.
     *
     * @param node    hostname segment (null-terminated UTF-8)
     * @param service NULL segment or service name
     * @param hints   segment allocated with {@link #ADDRINFO_LAYOUT}, or NULL
     * @param res     pointer-to-pointer output segment (ADDRESS-sized)
     * @return 0 on success, non-zero error code on failure
     */
    public static int getaddrinfo(MemorySegment node, MemorySegment service, MemorySegment hints, MemorySegment res)
            throws Throwable {
        return (int) getaddrinfo.invokeExact(node, service, hints, res);
    }

    /**
     * Calls {@code freeaddrinfo(struct addrinfo *res)}.
     *
     * @param res the addrinfo pointer returned by {@link #getaddrinfo}
     */
    public static void freeaddrinfo(MemorySegment res) throws Throwable {
        freeaddrinfo.invokeExact(res);
    }

    /**
     * Calls {@code gai_strerror(int errcode)} and returns the error string.
     *
     * @param errcode error code from {@link #getaddrinfo}
     * @param arena   arena to scope the string reinterpret
     * @return human-readable error string
     */
    public static String gaiStrerror(int errcode, Arena arena) throws Throwable {
        MemorySegment ptr = (MemorySegment) gai_strerror.invokeExact(errcode);
        return getStringFromNativePointer(ptr, arena);
    }

    /**
     * Reads {@code ai_canonname} from the first addrinfo result pointer.
     *
     * @param resPtr the raw pointer value stored in the output segment after {@link #getaddrinfo}
     * @param arena  arena to scope the reinterpret
     * @return the canonical name string, or {@code null} if the pointer is NULL
     */
    public static String addrinfoCanoname(MemorySegment resPtr, Arena arena) {
        if (resPtr == null || MemorySegment.NULL.equals(resPtr)) {
            return null;
        }
        MemorySegment addrinfo = MemorySegment.ofAddress(resPtr.address()).reinterpret(ADDRINFO_LAYOUT.byteSize(),
                arena, null);
        MemorySegment canonPtr = (MemorySegment) ADDRINFO_CANONNAME.get(addrinfo, 0L);
        return getStringFromNativePointer(canonPtr, arena);
    }

    /**
     * Calls {@code getrlimit(int resource, struct rlimit *rlim)}.
     *
     * @param resource resource constant (e.g. {@link #RLIMIT_NOFILE})
     * @param rlim     segment allocated with {@link #RLIMIT_LAYOUT}
     * @return 0 on success, -1 on error
     */
    public static int getrlimit(int resource, MemorySegment rlim) throws Throwable {
        return (int) getrlimit.invokeExact(resource, rlim);
    }

    /**
     * Reads {@code rlim_cur} from a rlimit segment.
     *
     * @param rlim segment populated by {@link #getrlimit(int, MemorySegment)}
     * @return the soft resource limit
     */
    public static long rlimitCur(MemorySegment rlim) {
        return (long) RLIMIT_CUR.get(rlim, 0L);
    }

    /**
     * Reads {@code rlim_max} from a rlimit segment.
     *
     * @param rlim segment populated by {@link #getrlimit(int, MemorySegment)}
     * @return the hard resource limit
     */
    public static long rlimitMax(MemorySegment rlim) {
        return (long) RLIMIT_MAX.get(rlim, 0L);
    }
}
