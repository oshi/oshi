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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
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
     * @return
     */
    int proc_pidinfo(int pid, int flavor, long arg, Structure buffer, int buffersize);
}
