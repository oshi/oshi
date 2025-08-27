/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.MemoryLayout.paddingLayout;
import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.StructLayout;

/**
 * Constants and structures defined in MacOS header files
 */
public interface MacSystem {

    // host_statistics()
    int HOST_LOAD_INFO = 1; // System loading stats
    int HOST_VM_INFO = 2; // Virtual memory stats
    int HOST_CPU_LOAD_INFO = 3; // CPU load stats

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
    int UINT64_SIZE = (int) JAVA_LONG.byteSize();
    int INT_SIZE = (int) JAVA_INT.byteSize();

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

    StructLayout PROC_BSD_INFO = structLayout(
            //
            JAVA_INT.withName("pbi_flags"), //
            JAVA_INT.withName("pbi_status"), //
            JAVA_INT.withName("pbi_xstatus"), //
            JAVA_INT.withName("pbi_pid"), //
            JAVA_INT.withName("pbi_ppid"), //
            JAVA_INT.withName("pbi_uid"), //
            JAVA_INT.withName("pbi_gid"), //
            JAVA_INT.withName("pbi_ruid"), //
            JAVA_INT.withName("pbi_rgid"), //
            JAVA_INT.withName("pbi_svuid"), //
            JAVA_INT.withName("pbi_svgid"), //
            JAVA_INT.withName("rfu_1"), //
            sequenceLayout(MAXCOMLEN, JAVA_BYTE).withName("pbi_comm"), //
            sequenceLayout(2 * MAXCOMLEN, JAVA_BYTE).withName("pbi_name"), //
            JAVA_INT.withName("pbi_nfiles"), //
            JAVA_INT.withName("pbi_pgid"), //
            JAVA_INT.withName("pbi_pjobc"), //
            JAVA_INT.withName("e_tdev"), //
            JAVA_INT.withName("e_tpgid"), //
            JAVA_INT.withName("pbi_nice"), //
            JAVA_LONG.withName("pbi_start_tvsec"), //
            JAVA_LONG.withName("pbi_start_tvusec") //
    );
    PathElement PBI_STATUS = groupElement("pbi_status");
    PathElement PBI_FLAGS = groupElement("pbi_flags");
    PathElement PBI_PPID = groupElement("pbi_ppid");
    PathElement PBI_UID = groupElement("pbi_uid");
    PathElement PBI_GID = groupElement("pbi_gid");
    PathElement PBI_COMM = groupElement("pbi_comm");
    PathElement PBI_NFILES = groupElement("pbi_nfiles");
    PathElement PBI_START_TVSEC = groupElement("pbi_start_tvsec");
    PathElement PBI_START_TVUSEC = groupElement("pbi_start_tvusec");

    StructLayout PROC_TASK_INFO = structLayout(
            //
            JAVA_LONG.withName("pti_virtual_size"), // virtual memory size (bytes)
            JAVA_LONG.withName("pti_resident_size"), // resident memory size (bytes)
            JAVA_LONG.withName("pti_total_user"), // total time (nanoseconds)
            JAVA_LONG.withName("pti_total_system"), //
            JAVA_LONG.withName("pti_threads_user"), // existing threads only
            JAVA_LONG.withName("pti_threads_system"), //
            JAVA_INT.withName("pti_policy"), // default policy for new threads
            JAVA_INT.withName("pti_faults"), // number of page faults
            JAVA_INT.withName("pti_pageins"), // number of actual pageins
            JAVA_INT.withName("pti_cow_faults"), // number of copy-on-write faults
            JAVA_INT.withName("pti_messages_sent"), // number of messages sent
            JAVA_INT.withName("pti_messages_received"), // number of messages received
            JAVA_INT.withName("pti_syscalls_mach"), // number of mach system calls
            JAVA_INT.withName("pti_syscalls_unix"), // number of unix system calls
            JAVA_INT.withName("pti_csw"), // number of context switches
            JAVA_INT.withName("pti_threadnum"), // number of threads in the task
            JAVA_INT.withName("pti_numrunning"), // number of running threads
            JAVA_INT.withName("pti_priority") // task priority
    );
    PathElement PTI_VIRTUAL_SIZE = groupElement("pti_virtual_size");
    PathElement PTI_RESIDENT_SIZE = groupElement("pti_resident_size");
    PathElement PTI_TOTAL_USER = groupElement("pti_total_user");
    PathElement PTI_TOTAL_SYSTEM = groupElement("pti_total_system");
    PathElement PTI_FAULTS = groupElement("pti_faults");
    PathElement PTI_PAGEINS = groupElement("pti_pageins");
    PathElement PTI_CSW = groupElement("pti_csw");
    PathElement PTI_THREADNUM = groupElement("pti_threadnum");
    PathElement PTI_PRIORITY = groupElement("pti_priority");

    StructLayout PROC_TASK_ALL_INFO = MemoryLayout.structLayout(
            //
            PROC_BSD_INFO.withName("pbsd"), //
            PROC_TASK_INFO.withName("ptinfo") //
    );
    PathElement PBSD = groupElement("pbsd");
    PathElement PTINFO = groupElement("ptinfo");

    StructLayout PASSWD = MemoryLayout.structLayout(
            //
            ADDRESS.withName("pw_name"), // user name
            ADDRESS.withName("pw_passwd"), // encrypted password
            JAVA_INT.withName("pw_uid"), // user uid
            JAVA_INT.withName("pw_gid"), // user gid
            JAVA_LONG.withName("pw_change"), // password change time
            ADDRESS.withName("pw_class"), // user access class
            ADDRESS.withName("pw_gecos"), // Honeywell login info
            ADDRESS.withName("pw_dir"), // home directory
            ADDRESS.withName("pw_shell"), // default shell
            JAVA_LONG.withName("pw_expire"), // account expiration
            ADDRESS.withName("pw_fields") // internal: fields filled in
    );

    StructLayout GROUP = MemoryLayout.structLayout(
            //
            ADDRESS.withName("gr_name"), // group name
            ADDRESS.withName("gr_passwd"), // group password
            ADDRESS.withName("gr_gid"), // group id
            ADDRESS.withName("gr_mem") // group members
    );

    StructLayout RUSAGEINFOV2 = MemoryLayout.structLayout(
            //
            sequenceLayout(16, JAVA_BYTE).withName("ri_uuid"), //
            JAVA_LONG.withName("ri_user_time"), //
            JAVA_LONG.withName("ri_system_time"), //
            JAVA_LONG.withName("ri_pkg_idle_wkups"), //
            JAVA_LONG.withName("ri_interrupt_wkups"), //
            JAVA_LONG.withName("ri_pageins"), //
            JAVA_LONG.withName("ri_wired_size"), //
            JAVA_LONG.withName("ri_resident_size"), //
            JAVA_LONG.withName("ri_phys_footprint"), //
            JAVA_LONG.withName("ri_proc_start_abstime"), //
            JAVA_LONG.withName("ri_proc_exit_abstime"), //
            JAVA_LONG.withName("ri_child_user_time"), //
            JAVA_LONG.withName("ri_child_system_time"), //
            JAVA_LONG.withName("ri_child_pkg_idle_wkups"), //
            JAVA_LONG.withName("ri_child_interrupt_wkups"), //
            JAVA_LONG.withName("ri_child_pageins"), //
            JAVA_LONG.withName("ri_child_elapsed_abstime"), //
            JAVA_LONG.withName("ri_diskio_bytesread"), //
            JAVA_LONG.withName("ri_diskio_byteswritten") //
    );
    PathElement RI_DISKIO_BYTESREAD = groupElement("ri_diskio_bytesread");
    PathElement RI_DISKIO_BYTESWRITTEN = groupElement("ri_diskio_byteswritten");

    StructLayout VNODE_INFO_PATH = MemoryLayout.structLayout(
            //
            paddingLayout(152 * 8), // vnode_info but we don't need its data
            sequenceLayout(MAXPATHLEN, JAVA_BYTE).withName("vip_path"));
    PathElement VIP_PATH = groupElement("vip_path");

    StructLayout VNODE_PATH_INFO = MemoryLayout.structLayout(
            //
            VNODE_INFO_PATH.withName("pvi_cdir"), //
            VNODE_INFO_PATH.withName("pvi_rdir") //
    );
    PathElement PVI_CDIR = groupElement("pvi_cdir");

    StructLayout TIMEVAL = structLayout(
            //
            JAVA_LONG.withName("tv_sec"), // seconds
            JAVA_INT.withName("tv_usec") // microseconds
    );

    StructLayout RLIMIT = structLayout(
            //
            JAVA_LONG.withName("rlim_cur"), // current (soft) limit
            JAVA_LONG.withName("rlim_max") // hard limit
    );
    MemoryLayout.PathElement RLIM_CUR = groupElement("rlim_cur");
    MemoryLayout.PathElement RLIM_MAX = groupElement("rlim_max");

    StructLayout STATFS = structLayout(//
            JAVA_INT.withName("f_bsize"), // fundamental file system block size
            JAVA_INT.withName("f_iosize"), // optimal transfer block size
            JAVA_LONG.withName("f_blocks"), // total data blocks in file system
            JAVA_LONG.withName("f_bfree"), // free blocks in fs
            JAVA_LONG.withName("f_bavail"), // free blocks avail to non-superuser
            JAVA_LONG.withName("f_files"), // total file nodes in file system
            JAVA_LONG.withName("f_ffree"), // free file nodes in fs
            sequenceLayout(2, JAVA_INT).withName("f_fsid"), // file system id
            JAVA_INT.withName("f_owner"), // user that mounted the filesystem
            JAVA_INT.withName("f_type"), // type of filesystem
            JAVA_INT.withName("f_flags"), // copy of mount exported flags
            JAVA_INT.withName("f_fssubtype"), // fs sub-type (flavor)
            sequenceLayout(MFSTYPENAMELEN, JAVA_BYTE).withName("f_fstypename"), // fs type name
            sequenceLayout(MAXPATHLEN, JAVA_BYTE).withName("f_mntonname"), // directory on which mounted
            sequenceLayout(MAXPATHLEN, JAVA_BYTE).withName("f_mntfromname"), // mounted filesystem
            paddingLayout(8 * 4).withName("f_reserved") // For future use
    );
    MemoryLayout.PathElement F_FILES = groupElement("f_files");
    MemoryLayout.PathElement F_FFREE = groupElement("f_ffree");
    MemoryLayout.PathElement F_FSTYPENAME = groupElement("f_fstypename");
    MemoryLayout.PathElement F_FLAGS = groupElement("f_flags");
    MemoryLayout.PathElement F_MNTONNAME = groupElement("f_mntonname");
    MemoryLayout.PathElement F_MNTFROMNAME = groupElement("f_mntfromname");
}
