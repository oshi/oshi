/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.jna.platform.mac;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.unix.CLibrary;

/**
 * Power Supply stats. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface SystemB extends CLibrary, com.sun.jna.platform.mac.SystemB {
    SystemB INSTANCE = Native.loadLibrary("System", SystemB.class);

    // params.h
    int MAXCOMLEN = 16;
    int MAXPATHLEN = 1024;
    int PROC_PIDPATHINFO_MAXSIZE = MAXPATHLEN * 4;

    // proc_info.h
    int PROC_ALL_PIDS = 1;
    int PROC_PIDTASKALLINFO = 2;
    int PROC_PIDTBSDINFO = 3;
    int PROC_PIDTASKINFO = 4;
    int PROC_PIDVNODEPATHINFO = 9;

    // length of fs type name including null
    int MFSTYPENAMELEN = 16;
    // length of buffer for returned name
    int MNAMELEN = MAXPATHLEN;

    // fsstat paths
    int MNT_WAIT = 0x0001;
    int MNT_NOWAIT = 0x0010;
    int MNT_DWAIT = 0x0100;

    // resource.h
    int RUSAGE_INFO_V2 = 2;

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
    }

    class VMMeter extends Structure {
        /*
         * General system activity.
         */
        public int v_swtch; /* context switches */
        public int v_trap; /* calls to trap */
        public int v_syscall; /* calls to syscall() */
        public int v_intr; /* device interrupts */
        public int v_soft; /* software interrupts */
        public int v_faults; /* total faults taken */
        /*
         * Virtual memory activity.
         */
        public int v_lookups; /* object cache lookups */
        public int v_hits; /* object cache hits */
        public int v_vm_faults; /* number of address memory faults */
        public int v_cow_faults; /* number of copy-on-writes */
        public int v_swpin; /* swapins */
        public int v_swpout; /* swapouts */
        public int v_pswpin; /* pages swapped in */
        public int v_pswpout; /* pages swapped out */
        public int v_pageins; /* number of pageins */
        public int v_pageouts; /* number of pageouts */
        public int v_pgpgin; /* pages paged in */
        public int v_pgpgout; /* pages paged out */
        public int v_intrans; /* intransit blocking page faults */
        public int v_reactivated; /*
                                   * number of pages reactivated from free list
                                   */
        public int v_rev; /* revolutions of the hand */
        public int v_scan; /* scans in page out daemon */
        public int v_dfree; /* pages freed by daemon */
        public int v_pfree; /* pages freed by exiting processes */
        public int v_zfod; /* pages zero filled on demand */
        public int v_nzfod; /* number of zfod's created */
        /*
         * Distribution of page usages.
         */
        public int v_page_size; /* page size in bytes */
        public int v_kernel_pages; /* number of pages in use by kernel */
        public int v_free_target; /* number of pages desired free */
        public int v_free_min; /* minimum number of pages desired free */
        public int v_free_count; /* number of pages free */
        public int v_wire_count; /* number of pages wired down */
        public int v_active_count; /* number of pages active */
        public int v_inactive_target; /* number of pages desired inactive */
        public int v_inactive_count; /* number of pages inactive */

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "v_swtch", "v_trap", "v_syscall", "v_intr", "v_soft", "v_faults",
                    "v_lookups", "v_hits", "v_vm_faults", "v_cow_faults", "v_swpin", "v_swpout", "v_pswpin",
                    "v_pswpout", "v_pageins", "v_pageouts", "v_pgpgin", "v_pgpgout", "v_intrans", "v_reactivated",
                    "v_rev", "v_scan", "v_dfree", "v_pfree", "v_zfod", "v_nzfod", "v_page_size", "v_kernel_pages",
                    "v_free_target", "v_free_min", "v_free_count", "v_wire_count", "v_active_count",
                    "v_inactive_target", "v_inactive_count" });
        }
    }

    class RUsageInfoV2 extends Structure {
        public byte[] ri_uuid = new byte[16];
        public long ri_user_time;
        public long ri_system_time;
        public long ri_pkg_idle_wkups;
        public long ri_interrupt_wkups;
        public long ri_pageins;
        public long ri_wired_size;
        public long ri_resident_size;
        public long ri_phys_footprint;
        public long ri_proc_start_abstime;
        public long ri_proc_exit_abstime;
        public long ri_child_user_time;
        public long ri_child_system_time;
        public long ri_child_pkg_idle_wkups;
        public long ri_child_interrupt_wkups;
        public long ri_child_pageins;
        public long ri_child_elapsed_abstime;
        public long ri_diskio_bytesread;
        public long ri_diskio_byteswritten;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ri_uuid", "ri_user_time", "ri_system_time", "ri_pkg_idle_wkups",
                    "ri_interrupt_wkups", "ri_pageins", "ri_wired_size", "ri_resident_size", "ri_phys_footprint",
                    "ri_proc_start_abstime", "ri_proc_exit_abstime", "ri_child_user_time", "ri_child_system_time",
                    "ri_child_pkg_idle_wkups", "ri_child_interrupt_wkups", "ri_child_pageins",
                    "ri_child_elapsed_abstime", "ri_diskio_bytesread", "ri_diskio_byteswritten" });
        }
    }

    class VnodeInfoPath extends Structure {
        public byte[] vip_vi = new byte[152]; // vnode_info but we don't
                                              // need its data
        public byte[] vip_path = new byte[MAXPATHLEN];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "vip_vi", "vip_path" });
        }
    }

    class VnodePathInfo extends Structure {
        public VnodeInfoPath pvi_cdir;
        public VnodeInfoPath pvi_rdir;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "pvi_cdir", "pvi_rdir" });
        }
    }

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
    }

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
    }

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
     * Return type for getpwuid
     */
    class Passwd extends Structure {
        public String pw_name; // user name
        public String pw_passwd; // encrypted password
        public int pw_uid; // user uid
        public int pw_gid; // user gid
        public NativeLong pw_change; // password change time
        public String pw_class; // user access class
        public String pw_gecos; // Honeywell login info
        public String pw_dir; // home directory
        public String pw_shell; // default shell
        public NativeLong pw_expire; // account expiration
        public int pw_fields; // internal: fields filled in

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "pw_name", "pw_passwd", "pw_uid", "pw_gid", "pw_change", "pw_class",
                    "pw_gecos", "pw_dir", "pw_shell", "pw_expire", "pw_fields" });
        }
    }

    /**
     * Return type for getgrgid
     */
    class Group extends Structure {
        public String gr_name; /* group name */
        public String gr_passwd; /* group password */
        public int gr_gid; /* group id */
        public PointerByReference gr_mem; /* group members */

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "gr_name", "gr_passwd", "gr_gid", "gr_mem" });
        }
    }

    /**
     * This function searches the password database for the given user uid,
     * always returning the first one encountered.
     *
     * @param uid
     *            The user ID
     * @return a Passwd structure matching that user
     */
    Passwd getpwuid(int uid);

    /**
     * This function searches the group database for the given group name
     * pointed to by the group id given by gid, returning the first one
     * encountered. Identical group gids may result in undefined behavior.
     *
     * @param gid
     *            The group ID
     * @return a Group structure matching that group
     */
    Group getgrgid(int gid);

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

    /**
     * Return resource usage information for the given pid, which can be a live
     * process or a zombie.
     *
     * @param pid
     *            the process identifier
     * @param flavor
     *            the type of information requested
     * @param buffer
     *            holds results
     * @return 0 on success; or -1 on failure, with errno set to indicate the
     *         specific error.
     */
    int proc_pid_rusage(int pid, int flavor, RUsageInfoV2 buffer);

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
}
