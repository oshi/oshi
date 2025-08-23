/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * Constants defined in MacOS header files
 */
public interface MacSystemHeaders {

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
}
