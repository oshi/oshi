/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.windows.perfmon;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants used in Perfmon driver classes
 */
@ThreadSafe
public final class PerfmonConstants {

    /*
     * Perfmon counter names and corresponding WMI tables
     */
    static final String MEMORY = "Memory";
    static final String WIN32_PERF_RAW_DATA_PERF_OS_MEMORY = "Win32_PerfRawData_PerfOS_Memory";

    static final String PAGING_FILE = "Paging File";
    static final String WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE = "Win32_PerfRawData_PerfOS_PagingFile";

    static final String PHYSICAL_DISK = "PhysicalDisk";
    static final String WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL = "Win32_PerfRawData_PerfDisk_PhysicalDisk WHERE Name!=\"_Total\"";

    static final String PROCESS = "Process";
    static final String WIN32_PERFPROC_PROCESS = "Win32_PerfRawData_PerfProc_Process";
    static final String WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL = WIN32_PERFPROC_PROCESS
            + " WHERE NOT Name LIKE \"%_Total\"";

    static final String THREAD = "Thread";
    static final String WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL = "Win32_PerfRawData_PerfProc_Thread WHERE NOT Name LIKE \"%_Total\"";

    // For Vista- ... Older systems just have processor #
    static final String PROCESSOR = "Processor";
    static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name!=\"_Total\"";
    static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"";

    // For Win7+ ... NAME field includes NUMA nodes
    static final String PROCESSOR_INFORMATION = "Processor Information";
    static final String WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL = "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE \"%_Total\"";

    static final String SYSTEM = "System";
    static final String WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM = "Win32_PerfRawData_PerfOS_System";

    /**
     * Everything in this class is static, never instantiate it
     */
    private PerfmonConstants() {
        throw new AssertionError();
    }
}
