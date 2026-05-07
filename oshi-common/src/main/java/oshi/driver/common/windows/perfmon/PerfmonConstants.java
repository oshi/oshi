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

    /** Instance filter: _Total instance only. */
    public static final String TOTAL_INSTANCE = "_Total";
    /** Instance filter: _Total or Idle instances. */
    public static final String TOTAL_OR_IDLE_INSTANCES = "_Total|Idle";
    /** Instance filter: all _Total instances (wildcard). */
    public static final String TOTAL_INSTANCES = "*_Total";
    /** Instance filter: exclude _Total instance. */
    public static final String NOT_TOTAL_INSTANCE = "^" + TOTAL_INSTANCE;
    /** Instance filter: exclude all _Total instances. */
    public static final String NOT_TOTAL_INSTANCES = "^" + TOTAL_INSTANCES;

    /** PDH object name for Memory counters. */
    public static final String MEMORY = "Memory";
    /** WMI class for raw Memory performance data. */
    public static final String WIN32_PERF_RAW_DATA_PERF_OS_MEMORY = "Win32_PerfRawData_PerfOS_Memory";

    /** PDH object name for Paging File counters. */
    public static final String PAGING_FILE = "Paging File";
    /** WMI class for raw Paging File performance data. */
    public static final String WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE = "Win32_PerfRawData_PerfOS_PagingFile";

    /** PDH object name for Physical Disk counters. */
    public static final String PHYSICAL_DISK = "PhysicalDisk";
    /** WMI class for raw Physical Disk data excluding _Total. */
    public static final String WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL = "Win32_PerfRawData_PerfDisk_PhysicalDisk WHERE Name!=\"_Total\"";

    /** PDH object name for Process counters. */
    public static final String PROCESS = "Process";
    /** WMI class for raw Process performance data. */
    public static final String WIN32_PERFPROC_PROCESS = "Win32_PerfRawData_PerfProc_Process";
    /** WMI query for Process data excluding _Total. */
    public static final String WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL = WIN32_PERFPROC_PROCESS
            + " WHERE NOT Name LIKE \"%_Total\"";
    /** WMI query for Process _Total data. */
    public static final String WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL = WIN32_PERFPROC_PROCESS
            + " WHERE Name=\"_Total\"";
    /** WMI query for the System Idle Process (PID 0). */
    public static final String WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0 = "Win32_PerfRawData_PerfProc_Process WHERE IDProcess=0";

    /** PDH object name for Thread counters. */
    public static final String THREAD = "Thread";
    /** WMI class for raw Thread performance data. */
    public static final String WIN32_PERF_RAW_DATA_PERF_PROC_THREAD = "Win32_PerfRawData_PerfProc_Thread";
    /** WMI query for Thread data excluding _Total. */
    public static final String WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL = "Win32_PerfRawData_PerfProc_Thread WHERE NOT Name LIKE \"%_Total\"";

    /** PDH object name for Processor counters (Vista and earlier). */
    public static final String PROCESSOR = "Processor";
    /** WMI query for Processor data excluding _Total. */
    public static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name!=\"_Total\"";
    /** WMI query for Processor _Total data. */
    public static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"";

    /** PDH object name for Processor Information counters (Win7+). */
    public static final String PROCESSOR_INFORMATION = "Processor Information";
    /** WMI query for raw Processor Information data excluding _Total. */
    public static final String WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL = "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE \"%_Total\"";
    /** WMI query for formatted (cooked) Processor Information data excluding _Total. */
    public static final String WIN32_PERF_FORMATTED_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL = "Win32_PerfFormattedData_Counters_ProcessorInformation WHERE NOT Name LIKE \"%_Total\"";

    /** PDH object name for System counters. */
    public static final String SYSTEM = "System";
    /** WMI class for raw System performance data. */
    public static final String WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM = "Win32_PerfRawData_PerfOS_System";

    /** PDH object name for GPU Engine counters. */
    public static final String GPU_ENGINE = "GPU Engine";
    /** PDH object name for GPU Adapter Memory counters. */
    public static final String GPU_ADAPTER_MEMORY = "GPU Adapter Memory";

    /**
     * Everything in this class is static, never instantiate it
     */
    private PerfmonConstants() {
        throw new AssertionError();
    }
}
