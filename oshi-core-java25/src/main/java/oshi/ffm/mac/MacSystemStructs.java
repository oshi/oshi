/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

/**
 * Structures defined in MacOS header files
 */
public interface MacSystemStructs {
    // Constants
    int MAXCOMLEN = 16;

    StructLayout PROC_TASK_INFO = structLayout( //
            JAVA_LONG.withName("pti_virtual_size"), // virtual memory size (bytes)
            JAVA_LONG.withName("pti_resident_size"), // resident memory size (bytes)
            JAVA_LONG.withName("pti_total_user"), // total time (nanoseconds)
            JAVA_LONG.withName("pti_total_system"), JAVA_LONG.withName("pti_threads_user"), // existing threads only
            JAVA_LONG.withName("pti_threads_system"), JAVA_INT.withName("pti_policy"), // default policy for new threads
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

    MemoryLayout.PathElement PTI_THREADNUM = groupElement("pti_threadnum");

    StructLayout TIMEVAL = structLayout( //
            JAVA_LONG.withName("tv_sec"), // seconds
            JAVA_INT.withName("tv_usec") // microseconds
    );

    StructLayout RLIMIT = structLayout( //
            JAVA_LONG.withName("rlim_cur"), // current (soft) limit
            JAVA_LONG.withName("rlim_max") // hard limit
    );
    MemoryLayout.PathElement RLIM_CUR = groupElement("rlim_cur");
    MemoryLayout.PathElement RLIM_MAX = groupElement("rlim_max");
}
