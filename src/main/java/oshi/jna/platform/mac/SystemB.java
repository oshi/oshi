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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.mac;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Structure;

/**
 * Power Supply stats. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface SystemB extends com.sun.jna.platform.mac.SystemB {
    SystemB INSTANCE = (SystemB) Native.loadLibrary("System", SystemB.class);

    // proc_info.h
    final static int PROC_ALL_PIDS = 1;
    final static int PROC_PIDTASKINFO = 4;

    static class ProcTaskInfo extends Structure {
        public long pti_virtual_size; /* virtual memory size (bytes) */
        public long pti_resident_size; /* resident memory size (bytes) */
        public long pti_total_user; /* total time */
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
    static final int MAXPATHLEN = 1024;
    static final int MFSTYPENAMELEN = 16;
    // length of buffer for returned name
    static final int MNAMELEN = MAXPATHLEN;

    /**
     * The statfs() routine returns information about a mounted file system. The
     * path argument is the path name of any file or directory within the
     * mounted file system. The buf argument is a pointer to a statfs structure.
     */
    static class Statfs extends Structure {
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
    static class XswUsage extends Structure {
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
    static class Timeval extends Structure {
        public long tv_sec; // seconds
        public long tv_usec; // microseconds

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "tv_sec", "tv_usec" });
        }
    };

    int mach_task_self();

    // Native call for getting load average
    int getloadavg(double[] loadavg, int nelem);

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

    static final int MNT_WAIT = 0x0001;
    static final int MNT_NOWAIT = 0x0010;
    static final int MNT_DWAIT = 0x0100;

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
