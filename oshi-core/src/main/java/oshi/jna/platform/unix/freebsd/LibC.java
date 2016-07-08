/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.unix.freebsd;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * Power Supply stats. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface LibC extends Library {
    LibC INSTANCE = (LibC) Native.loadLibrary("libc", LibC.class);

    // params.h
    int MAXCOMLEN = 16;
    int MAXPATHLEN = 1024;
    int PROC_PIDPATHINFO_MAXSIZE = MAXPATHLEN * 4;

    // proc_info.h
    int PROC_ALL_PIDS = 1;
    int PROC_PIDTASKALLINFO = 2;
    int PROC_PIDTBSDINFO = 3;
    int PROC_PIDTASKINFO = 4;

    class ProcTaskAllInfo extends Structure {
        public ProcBsdInfo pbsd;
        public ProcTaskInfo ptinfo;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "pbsd", "ptinfo" });
        }
    }

    class ProcBsdInfo extends Structure {
        public int pbi_flags;
        public int pbi_status;
        public int pbi_xstatus;
        public int pbi_pid;
        public int pbi_ppid;
        public int pbi_uid;
        public int pbi_gid;
        public int pbi_ruid;
        public int pbi_rgid;
        public int pbi_svuid;
        public int pbi_svgid;
        public int rfu_1;
        public byte[] pbi_comm = new byte[MAXCOMLEN];
        public byte[] pbi_name = new byte[2 * MAXCOMLEN];
        public int pbi_nfiles;
        public int pbi_pgid;
        public int pbi_pjobc;
        public int e_tdev;
        public int e_tpgid;
        public int pbi_nice;
        public long pbi_start_tvsec;
        public long pbi_start_tvusec;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "pbi_flags", "pbi_status", "pbi_xstatus", "pbi_pid", "pbi_ppid",
                    "pbi_uid", "pbi_gid", "pbi_ruid", "pbi_rgid", "pbi_svuid", "pbi_svgid", "rfu_1", "pbi_comm",
                    "pbi_name", "pbi_nfiles", "pbi_pgid", "pbi_pjobc", "e_tdev", "e_tpgid", "pbi_nice",
                    "pbi_start_tvsec", "pbi_start_tvusec" });
        }
    }

    class ProcTaskInfo extends Structure {
        public long pti_virtual_size; /* virtual memory size (bytes) */
        public long pti_resident_size; /* resident memory size (bytes) */
        public long pti_total_user; /* total time (nanoseconds) */
        public long pti_total_system;
        public long pti_threads_user; /* existing threads only */
        public long pti_threads_system;
        public int pti_policy; /* default policy for new threads */
        public int pti_faults; /* number of page faults */
        public int pti_pageins; /* number of actual pageins */
        public int pti_cow_faults; /* number of copy-on-write faults */
        public int pti_messages_sent; /* number of messages sent */
        public int pti_messages_received; /* number of messages received */
        public int pti_syscalls_mach; /* number of mach system calls */
        public int pti_syscalls_unix; /* number of unix system calls */
        public int pti_csw; /* number of context switches */
        public int pti_threadnum; /* number of threads in the task */
        public int pti_numrunning; /* number of running threads */
        public int pti_priority; /* task priority */

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "pti_virtual_size", "pti_resident_size", "pti_total_user",
                    "pti_total_system", "pti_threads_user", "pti_threads_system", "pti_policy", "pti_faults",
                    "pti_pageins", "pti_cow_faults", "pti_messages_sent", "pti_messages_received", "pti_syscalls_mach",
                    "pti_syscalls_unix", "pti_csw", "pti_threadnum", "pti_numrunning", "pti_priority" });
        }
    };

    // length of fs type name including null
    int MFSTYPENAMELEN = 16;
    // length of buffer for returned name
    int MNAMELEN = MAXPATHLEN;

    /**
     * The statfs() routine returns information about a mounted file system. The
     * path argument is the path name of any file or directory within the
     * mounted file system. The buf argument is a pointer to a statfs structure.
     */
    class Statfs extends Structure {
        public int f_bsize; /* fundamental file system block size */
        public int f_iosize; /* optimal transfer block size */
        public long f_blocks; /* total data blocks in file system */
        public long f_bfree; /* free blocks in fs */
        public long f_bavail; /* free blocks avail to non-superuser */
        public long f_files; /* total file nodes in file system */
        public long f_ffree; /* free file nodes in fs */
        public int[] f_fsid = new int[2]; /* file system id */
        public int f_owner; /* user that mounted the filesystem */
        public int f_type; /* type of filesystem */
        public int f_flags; /* copy of mount exported flags */
        public int f_fssubtype; /* fs sub-type (flavor) */
        /* fs type name */
        public byte[] f_fstypename = new byte[MFSTYPENAMELEN];
        /* directory on which mounted */
        public byte[] f_mntonname = new byte[MAXPATHLEN];
        /* mounted filesystem */
        public byte[] f_mntfromname = new byte[MAXPATHLEN];
        public int[] f_reserved = new int[8]; /* For future use */

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "f_bsize", "f_iosize", "f_blocks", "f_bfree", "f_bavail", "f_files",
                    "f_ffree", "f_fsid", "f_owner", "f_type", "f_flags", "f_fssubtype", "f_fstypename", "f_mntonname",
                    "f_mntfromname", "f_reserved" });
        }
    };

    /**
     * Return type for sysctl vm.swapusage
     */
    class XswUsage extends Structure {
        public long xsu_total;
        public long xsu_avail;
        public long xsu_used;
        public int xsu_pagesize;
        public boolean xsu_encrypted;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays
                    .asList(new String[] { "xsu_total", "xsu_avail", "xsu_used", "xsu_pagesize", "xsu_encrypted" });
        }
    };

    /**
     * Return type for sysctl kern.boottime
     */
    class Timeval extends Structure {
        public int tv_sec; // seconds
        public int tv_usec; // microseconds

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "tv_sec", "tv_usec" });
        }
    };

    /**
     * Data type as part of IFmsgHdr
     */
    class IFdata extends Structure {
        public byte ifi_type; // ethernet, tokenring, etc
        public byte ifi_typelen; // Length of frame type id
        public byte ifi_physical; // e.g., AUI, Thinnet, 10base-T, etc
        public byte ifi_addrlen; // media address length
        public byte ifi_hdrlen; // media header length
        public byte ifi_recvquota; // polling quota for receive intrs
        public byte ifi_xmitquota; // polling quota for xmit intrs
        public byte ifi_unused1; // for future use
        public int ifi_mtu; // maximum transmission unit
        public int ifi_metric; // routing metric (external only)
        public int ifi_baudrate; // linespeed
        public int ifi_ipackets; // packets received on interface
        public int ifi_ierrors; // input errors on interface
        public int ifi_opackets; // packets sent on interface
        public int ifi_oerrors; // output errors on interface
        public int ifi_collisions; // collisions on csma interfaces
        public int ifi_ibytes; // total number of octets received
        public int ifi_obytes; // total number of octets sent
        public int ifi_imcasts; // packets received via multicast
        public int ifi_omcasts; // packets sent via multicast
        public int ifi_iqdrops; // dropped on input, this interface
        public int ifi_noproto; // destined for unsupported protocol
        public int ifi_recvtiming; // usec spent receiving when timing
        public int ifi_xmittiming; // usec spent xmitting when timing
        public Timeval ifi_lastchange; // time of last administrative change
        public int ifi_unused2; // used to be the default_proto
        public int ifi_hwassist; // HW offload capabilities
        public int ifi_reserved1; // for future use
        public int ifi_reserved2; // for future use

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ifi_type", "ifi_typelen", "ifi_physical", "ifi_addrlen", "ifi_hdrlen",
                    "ifi_recvquota", "ifi_xmitquota", "ifi_unused1", "ifi_mtu", "ifi_metric", "ifi_baudrate",
                    "ifi_ipackets", "ifi_ierrors", "ifi_opackets", "ifi_oerrors", "ifi_collisions", "ifi_ibytes",
                    "ifi_obytes", "ifi_imcasts", "ifi_omcasts", "ifi_iqdrops", "ifi_noproto", "ifi_recvtiming",
                    "ifi_xmittiming", "ifi_lastchange", "ifi_unused2", "ifi_hwassist", "ifi_reserved1",
                    "ifi_reserved2" });
        }
    }

    /**
     * Return type for sysctl CTL_NET,PF_ROUTE
     */
    class IFmsgHdr extends Structure {
        public short ifm_msglen; // to skip over non-understood messages
        public byte ifm_version; // future binary compatability
        public byte ifm_type; // message type
        public int ifm_addrs; // like rtm_addrs
        public int ifm_flags; // value of if_flags
        public short ifm_index; // index for associated ifp
        public IFdata ifm_data; // statistics and other data about if

        public IFmsgHdr() {
            super();
        }

        public IFmsgHdr(Pointer p) {
            super(p);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ifm_msglen", "ifm_version", "ifm_type", "ifm_addrs", "ifm_flags",
                    "ifm_index", "ifm_data" });
        }
    }

    /**
     * Data type as part of IFmsgHdr
     */
    class IFdata64 extends Structure {
        public byte ifi_type; // ethernet, tokenring, etc
        public byte ifi_typelen; // Length of frame type id
        public byte ifi_physical; // e.g., AUI, Thinnet, 10base-T, etc
        public byte ifi_addrlen; // media address length
        public byte ifi_hdrlen; // media header length
        public byte ifi_recvquota; // polling quota for receive intrs
        public byte ifi_xmitquota; // polling quota for xmit intrs
        public byte ifi_unused1; // for future use
        public int ifi_mtu; // maximum transmission unit
        public int ifi_metric; // routing metric (external only)
        public long ifi_baudrate; // linespeed
        public long ifi_ipackets; // packets received on interface
        public long ifi_ierrors; // input errors on interface
        public long ifi_opackets; // packets sent on interface
        public long ifi_oerrors; // output errors on interface
        public long ifi_collisions; // collisions on csma interfaces
        public long ifi_ibytes; // total number of octets received
        public long ifi_obytes; // total number of octets sent
        public long ifi_imcasts; // packets received via multicast
        public long ifi_omcasts; // packets sent via multicast
        public long ifi_iqdrops; // dropped on input, this interface
        public long ifi_noproto; // destined for unsupported protocol
        public int ifi_recvtiming; // usec spent receiving when timing
        public int ifi_xmittiming; // usec spent xmitting when timing
        public Timeval ifi_lastchange; // time of last administrative change

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ifi_type", "ifi_typelen", "ifi_physical", "ifi_addrlen", "ifi_hdrlen",
                    "ifi_recvquota", "ifi_xmitquota", "ifi_unused1", "ifi_mtu", "ifi_metric", "ifi_baudrate",
                    "ifi_ipackets", "ifi_ierrors", "ifi_opackets", "ifi_oerrors", "ifi_collisions", "ifi_ibytes",
                    "ifi_obytes", "ifi_imcasts", "ifi_omcasts", "ifi_iqdrops", "ifi_noproto", "ifi_recvtiming",
                    "ifi_xmittiming", "ifi_lastchange" });
        }
    }

    /**
     * Return type for sysctl CTL_NET,PF_ROUTE
     */
    class IFmsgHdr2 extends Structure {
        public short ifm_msglen; // to skip over non-understood messages
        public byte ifm_version; // future binary compatability
        public byte ifm_type; // message type
        public int ifm_addrs; // like rtm_addrs
        public int ifm_flags; // value of if_flags
        public short ifm_index; // index for associated ifp
        public int ifm_snd_len; // instantaneous length of send queue
        public int ifm_snd_maxlen; // maximum length of send queue
        public int ifm_snd_drops; // number of drops in send queue
        public int ifm_timer; // time until if_watchdog called
        public IFdata64 ifm_data; // statistics and other data about if

        public IFmsgHdr2(Pointer p) {
            super(p);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ifm_msglen", "ifm_version", "ifm_type", "ifm_addrs", "ifm_flags",
                    "ifm_index", "ifm_snd_len", "ifm_snd_maxlen", "ifm_snd_drops", "ifm_timer", "ifm_data" });
        }
    }

    /**
     * Search through the current processes
     * 
     * @param type
     *            types of processes to be searched
     * @param typeinfo
     *            adjunct information for type
     * @param buffer
     *            a C array of int-sized values to be filled with process
     *            identifiers that hold an open file reference matching the
     *            specified path or volume. Pass NULL to obtain the minimum
     *            buffer size needed to hold the currently active processes.
     * @param buffersize
     *            the size (in bytes) of the provided buffer.
     * @return the number of bytes of data returned in the provided buffer; -1
     *         if an error was encountered;
     */
    int proc_listpids(int type, int typeinfo, int[] buffer, int buffersize);

    /**
     * Return in buffer a proc_*info structure corresponding to the flavor for
     * the specified process
     * 
     * @param pid
     *            the process identifier
     * @param flavor
     *            the type of information requested
     * @param arg
     *            argument possibly needed for some flavors
     * @param buffer
     *            holds results
     * @param buffersize
     *            size of results
     * @return the number of bytes of data returned in the provided buffer; -1
     *         if an error was encountered;
     */
    int proc_pidinfo(int pid, int flavor, long arg, Structure buffer, int buffersize);

    /**
     * Return in buffer the name of the specified process
     * 
     * @param pid
     *            the process identifier
     * @param buffer
     *            holds results
     * @param buffersize
     *            size of results
     * @return the length of the name returned in buffer if successful; 0
     *         otherwise
     */
    int proc_pidpath(int pid, Pointer buffer, int buffersize);

    int MNT_WAIT = 0x0001;
    int MNT_NOWAIT = 0x0010;
    int MNT_DWAIT = 0x0100;

    /**
     * The getfsstat() function returns information about all mounted file
     * systems. The buf argument is a pointer to an array of statfs structures.
     * 
     * Fields that are undefined for a particular file system are set to -1. The
     * buffer is filled with an array of statfs structures, one for each mounted
     * file system up to the size specified by bufsize.
     * 
     * @param buf
     *            Array of statfs structures that will be filled with results.
     *            If buf is given as NULL, getfsstat() returns just the number
     *            of mounted file systems.
     * @param bufsize
     *            Size of the buffer to fill
     * @param flags
     *            If flags is set to MNT_NOWAIT, getfsstat() will directly
     *            return the information retained in the kernel to avoid delays
     *            caused by waiting for updated information from a file system
     *            that is perhaps temporarily unable to respond. Some of the
     *            information returned may be out of date, however; if flags is
     *            set to MNT_WAIT or MNT_DWAIT instead, getfsstat() will request
     *            updated information from each mounted filesystem before
     *            returning.
     * @return Upon successful completion, the number of statfs structures is
     *         returned. Otherwise, -1 is returned and the global variable errno
     *         is set to indicate the error.
     */
    int getfsstat64(Statfs[] buf, int bufsize, int flags);

    /**
     * Returns the process ID of the calling process. The ID is guaranteed to be
     * unique and is useful for constructing temporary file names.
     * 
     * @return the process ID of the calling process.
     */
    int getpid();

    // host_statistics()
    int HOST_LOAD_INFO = 1;// System loading stats
    int HOST_VM_INFO = 2; // Virtual memory stats
    int HOST_CPU_LOAD_INFO = 3;// CPU load stats

    // host_statistics64()
    int HOST_VM_INFO64 = 4; // 64-bit virtual memory stats

    // host_cpu_load_info()
    int CPU_STATE_MAX = 4;
    int CPU_STATE_USER = 0;
    int CPU_STATE_SYSTEM = 1;
    int CPU_STATE_IDLE = 2;
    int CPU_STATE_NICE = 3;

    // host_processor_info() flavor
    int PROCESSOR_BASIC_INFO = 1;
    int PROCESSOR_CPU_LOAD_INFO = 2;

    // Data size
    int UINT64_SIZE = Native.getNativeSize(long.class);
    int INT_SIZE = Native.getNativeSize(int.class);

    public static class HostCpuLoadInfo extends Structure {
        public int cpu_ticks[] = new int[CPU_STATE_MAX];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "cpu_ticks" });
        }
    }

    public static class HostLoadInfo extends Structure {
        public int[] avenrun = new int[3]; // scaled by LOAD_SCALE
        public int[] mach_factor = new int[3]; // scaled by LOAD_SCALE

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "avenrun", "mach_factor" });
        }
    }

    public static class VMStatistics extends Structure {
        public int free_count; // # of pages free
        public int active_count; // # of pages active
        public int inactive_count; // # of pages inactive
        public int wire_count; // # of pages wired down
        public int zero_fill_count; // # of zero fill pages
        public int reactivations; // # of pages reactivated
        public int pageins; // # of pageins
        public int pageouts; // # of pageouts
        public int faults; // # of faults
        public int cow_faults; // # of copy-on-writes
        public int lookups; // object cache lookups
        public int hits; // object cache hits
        public int purgeable_count; // # of pages purgeable
        public int purges; // # of pages purged
        // # of pages speculative (included in free_count)
        public int speculative_count;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "free_count", "active_count", "inactive_count", "wire_count",
                    "zero_fill_count", "reactivations", "pageins", "pageouts", "faults", "cow_faults", "lookups",
                    "hits", "purgeable_count", "purges", "speculative_count" });
        }
    }

    public static class VMStatistics64 extends Structure {
        public int free_count; // # of pages free
        public int active_count; // # of pages active
        public int inactive_count; // # of pages inactive
        public int wire_count; // # of pages wired down
        public long zero_fill_count; // # of zero fill pages
        public long reactivations; // # of pages reactivated
        public long pageins; // # of pageins
        public long pageouts; // # of pageouts
        public long faults; // # of faults
        public long cow_faults; // # of copy-on-writes
        public long lookups; // object cache lookups
        public long hits; // object cache hits
        public long purges; // # of pages purged
        public int purgeable_count;
        public int speculative_count;
        public long decompressions; // # of pages decompressed
        public long compressions; // # of pages compressed
        // # of pages swapped in (via compression segments)
        public long swapins;
        // # of pages swapped out (via compression segments)
        public long swapouts;
        // # of pages used by the compressed pager to hold all the
        // compressed data
        public int compressor_page_count;
        public int throttled_count; // # of pages throttled
        // # of pages that are file-backed (non-swap)
        public int external_page_count;
        public int internal_page_count; // # of pages that are anonymous
        // # of pages (uncompressed) held within the compressor.
        public long total_uncompressed_pages_in_compressor;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "free_count", "active_count", "inactive_count", "wire_count",
                    "zero_fill_count", "reactivations", "pageins", "pageouts", "faults", "cow_faults", "lookups",
                    "hits", "purges", "purgeable_count", "speculative_count", "decompressions", "compressions",
                    "swapins", "swapouts", "compressor_page_count", "throttled_count", "external_page_count",
                    "internal_page_count", "total_uncompressed_pages_in_compressor" });
        }
    }

    /**
     * The mach_host_self system call returns the calling thread's host name
     * port. It has an effect equivalent to receiving a send right for the host
     * port.
     *
     * @return the host's name port
     */
    int mach_host_self();

    /**
     * The mach_task_self system call returns the calling thread's task_self
     * port. It has an effect equivalent to receiving a send right for the
     * task's kernel port.
     *
     * @return the task's kernel port
     */
    int mach_task_self();

    /**
     * The host_page_size function returns the page size for the given host.
     *
     * @param machPort
     *            The name (or control) port for the host for which the page
     *            size is desired.
     * @param pPageSize
     *            The host's page size (in bytes), set on success.
     * @return 0 on success; sets errno on failure
     */
    int host_page_size(int machPort, LongByReference pPageSize);

    /**
     * The host_statistics function returns scheduling and virtual memory
     * statistics concerning the host as specified by hostStat.
     *
     * @param machPort
     *            The control port for the host for which information is to be
     *            obtained.
     * @param hostStat
     *            The type of statistics desired (HOST_LOAD_INFO, HOST_VM_INFO,
     *            or HOST_CPU_LOAD_INFO)
     * @param stats
     *            Statistics about the specified host.
     * @param count
     *            On input, the maximum size of the buffer; on output, the size
     *            returned (in natural-sized units).
     * @return 0 on success; sets errno on failure
     */
    int host_statistics(int machPort, int hostStat, Structure stats, IntByReference count);

    /**
     * The host_statistics64 function returns 64-bit virtual memory statistics
     * concerning the host as specified by hostStat.
     *
     * @param machPort
     *            The control port for the host for which information is to be
     *            obtained.
     * @param hostStat
     *            The type of statistics desired (HOST_VM_INFO64)
     * @param stats
     *            Statistics about the specified host.
     * @param count
     *            On input, the maximum size of the buffer; on output, the size
     *            returned (in natural-sized units).
     * @return 0 on success; sets errno on failure
     */
    int host_statistics64(int machPort, int hostStat, Structure stats, IntByReference count);

    /**
     * The sysctl() function retrieves system information and allows processes
     * with appropriate privileges to set system information. The information
     * available from sysctl() consists of integers, strings, and tables.
     *
     * The state is described using a "Management Information Base" (MIB) style
     * name, listed in name, which is a namelen length array of integers.
     *
     * The information is copied into the buffer specified by oldp. The size of
     * the buffer is given by the location specified by oldlenp before the call,
     * and that location gives the amount of data copied after a successful call
     * and after a call that returns with the error code ENOMEM. If the amount
     * of data available is greater than the size of the buffer supplied, the
     * call supplies as much data as fits in the buffer provided and returns
     * with the error code ENOMEM. If the old value is not desired, oldp and
     * oldlenp should be set to NULL.
     *
     * The size of the available data can be determined by calling sysctl() with
     * the NULL argument for oldp. The size of the available data will be
     * returned in the location pointed to by oldlenp. For some operations, the
     * amount of space may change often. For these operations, the system
     * attempts to round up so that the returned size is large enough for a call
     * to return the data shortly thereafter.
     *
     * To set a new value, newp is set to point to a buffer of length newlen
     * from which the requested value is to be taken. If a new value is not to
     * be set, newp should be set to NULL and newlen set to 0.
     *
     * @param name
     *            MIB array of integers
     * @param namelen
     *            length of the MIB array
     * @param oldp
     *            Information retrieved
     * @param oldlenp
     *            Size of information retrieved
     * @param newp
     *            Information to be written
     * @param newlen
     *            Size of information to be written
     * @return 0 on success; sets errno on failure
     */
    int sysctl(int[] name, int namelen, Pointer oldp, IntByReference oldlenp, Pointer newp, int newlen);

    /**
     * The sysctlbyname() function accepts an ASCII representation of the name
     * and internally looks up the integer name vector. Apart from that, it
     * behaves the same as the standard sysctl() function.
     *
     * @param name
     *            ASCII representation of the MIB name
     * @param oldp
     *            Information retrieved
     * @param oldlenp
     *            Size of information retrieved
     * @param newp
     *            Information to be written
     * @param newlen
     *            Size of information to be written
     * @return 0 on success; sets errno on failure
     */
    int sysctlbyname(String name, Pointer oldp, IntByReference oldlenp, Pointer newp, int newlen);

    /**
     * The sysctlnametomib() function accepts an ASCII representation of the
     * name, looks up the integer name vector, and returns the numeric
     * representation in the mib array pointed to by mibp. The number of
     * elements in the mib array is given by the location specified by sizep
     * before the call, and that location gives the number of entries copied
     * after a successful call. The resulting mib and size may be used in
     * subsequent sysctl() calls to get the data associated with the requested
     * ASCII name. This interface is intended for use by applications that want
     * to repeatedly request the same variable (the sysctl() function runs in
     * about a third the time as the same request made via the sysctlbyname()
     * function).
     *
     * The number of elements in the mib array can be determined by calling
     * sysctlnametomib() with the NULL argument for mibp.
     *
     * The sysctlnametomib() function is also useful for fetching mib prefixes.
     * If size on input is greater than the number of elements written, the
     * array still contains the additional elements which may be written
     * programmatically.
     *
     * @param name
     *            ASCII representation of the name
     * @param mibp
     *            Integer array containing the corresponding name vector.
     * @param size
     *            On input, number of elements in the returned array; on output,
     *            the number of entries copied.
     * @return 0 on success; sets errno on failure
     */
    int sysctlnametomib(String name, Pointer mibp, IntByReference size);

    /**
     * The host_processor_info function returns information about processors.
     *
     * @param machPort
     *            The control port for the host for which information is to be
     *            obtained.
     * @param flavor
     *            The type of information requested.
     * @param procCount
     *            Pointer to the number of processors
     * @param procInfo
     *            Pointer to the structure corresponding to the requested flavor
     * @param procInfoCount
     *            Pointer to number of elements in the returned structure
     * @return 0 on success; sets errno on failure
     */
    int host_processor_info(int machPort, int flavor, IntByReference procCount, PointerByReference procInfo,
            IntByReference procInfoCount);

    /**
     * The getloadavg() function returns the number of processes in the system
     * run queue averaged over various periods of time. Up to nelem samples are
     * retrieved and assigned to successive elements of loadavg[]. The system
     * imposes a maximum of 3 samples, representing averages over the last 1, 5,
     * and 15 minutes, respectively.
     * 
     * @param loadavg
     *            An array of doubles which will be filled with the results
     * @param nelem
     *            Number of samples to return
     * @return If the load average was unobtainable, -1 is returned; otherwise,
     *         the number of samples actually retrieved is returned.
     * @see <A HREF=
     *      "https://www.freebsd.org/cgi/man.cgi?query=getloadavg&sektion=3">
     *      getloadavg(3)</A>
     */
    int getloadavg(double[] loadavg, int nelem);
}
