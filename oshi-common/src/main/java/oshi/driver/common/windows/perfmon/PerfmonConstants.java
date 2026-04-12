/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants used in Perfmon driver classes
 */
@ThreadSafe
public final class PerfmonConstants {

    /*
     * Perfmon counter names and corresponding WMI tables
     */
    public static final String MEMORY = "Memory";
    public static final String WIN32_PERF_RAW_DATA_PERF_OS_MEMORY = "Win32_PerfRawData_PerfOS_Memory";

    public static final String PAGING_FILE = "Paging File";
    public static final String WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE = "Win32_PerfRawData_PerfOS_PagingFile";

    public static final String PHYSICAL_DISK = "PhysicalDisk";
    public static final String WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL = "Win32_PerfRawData_PerfDisk_PhysicalDisk WHERE Name!=\"_Total\"";

    public static final String PROCESS = "Process";
    public static final String WIN32_PERFPROC_PROCESS = "Win32_PerfRawData_PerfProc_Process";
    public static final String WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL = WIN32_PERFPROC_PROCESS
            + " WHERE NOT Name LIKE \"%_Total\"";
    public static final String WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0 = "Win32_PerfRawData_PerfProc_Process WHERE IDProcess=0";

    public static final String THREAD = "Thread";
    public static final String WIN32_PERF_RAW_DATA_PERF_PROC_THREAD = "Win32_PerfRawData_PerfProc_Thread";
    public static final String WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL = "Win32_PerfRawData_PerfProc_Thread WHERE NOT Name LIKE \"%_Total\"";

    // For Vista- ... Older systems just have processor #
    public static final String PROCESSOR = "Processor";
    public static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name!=\"_Total\"";
    public static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"";

    // For Win7+ ... NAME field includes NUMA nodes
    public static final String PROCESSOR_INFORMATION = "Processor Information";
    public static final String WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL = "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE \"%_Total\"";

    public static final String SYSTEM = "System";
    public static final String WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM = "Win32_PerfRawData_PerfOS_System";

    /**
     * Everything in this class is static, never instantiate it
     */
    private PerfmonConstants() {
        throw new AssertionError();
    }
}
