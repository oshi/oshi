/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.VersionHelpers;

import oshi.driver.windows.perfmon.MemoryInformation.PageSwapProperty;
import oshi.driver.windows.perfmon.PagingFile.PagingPercentProperty;
import oshi.driver.windows.perfmon.PhysicalDisk.PhysicalDiskProperty;
import oshi.driver.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.driver.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.driver.windows.perfmon.ProcessorInformation.InterruptsProperty;
import oshi.driver.windows.perfmon.ProcessorInformation.ProcessorFrequencyProperty;
import oshi.driver.windows.perfmon.ProcessorInformation.ProcessorTickCountProperty;
import oshi.driver.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.windows.perfmon.ThreadInformation.ThreadPerformanceProperty;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.util.tuples.Pair;

class PerfrmonDriversTest {

    private static final String MEMORY = "Memory";
    private static final String WIN32_PERF_RAW_DATA_PERF_OS_MEMORY = "Win32_PerfRawData_PerfOS_Memory";

    private static final String PAGING_FILE = "Paging File";
    private static final String WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE = "Win32_PerfRawData_PerfOS_PagingFile";

    private static final String PHYSICAL_DISK = "PhysicalDisk";
    private static final String WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL = "Win32_PerfRawData_PerfDisk_PhysicalDisk WHERE Name!=\"_Total\"";

    private static final String WIN32_PERFPROC_PROCESS = "Win32_PerfRawData_PerfProc_Process";
    private static final String PROCESS = "Process";
    private static final String WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL = WIN32_PERFPROC_PROCESS
            + " WHERE NOT Name LIKE \"%_Total\"";

    private static final String PROCESSOR = "Processor";
    private static final String PROCESSOR_INFORMATION = "Processor Information";

    // For Win7+ ... NAME field includes NUMA nodes
    private static final String WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL = "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE \"%_Total\"";

    // For Vista- ... Older systems just have processor #
    private static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name!=\"_Total\"";
    private static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"";

    private static final String SYSTEM = "System";
    private static final String WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM = "Win32_PerfRawData_PerfOS_System";

    private static final String THREAD = "Thread";
    private static final String WIN32_PERF_RAW_DATA_PERF_PROC_THREAD = "Win32_PerfRawData_PerfProc_Thread WHERE NOT Name LIKE \"%_Total\"";

    @Test
    void testQueryPageSwaps() {
        if (Platform.isWindows()) {
            assertThat("Failed Combined queryPageSwaps", MemoryInformation.queryPageSwaps(),
                    is(aMapWithSize(PageSwapProperty.values().length)));
            assertThat("Failed PDH queryPageSwaps", PerfCounterQuery.queryValuesFromPDH(PageSwapProperty.class, MEMORY),
                    is(aMapWithSize(PageSwapProperty.values().length)));
            assertThat("Failed WMI queryPageSwaps",
                    PerfCounterQuery.queryValuesFromWMI(PageSwapProperty.class, WIN32_PERF_RAW_DATA_PERF_OS_MEMORY),
                    is(aMapWithSize(PageSwapProperty.values().length)));
        }
    }

    @Test
    void testQuerySwapUsed() {
        if (Platform.isWindows()) {
            assertThat("Failed Combined querySwapUsed", PagingFile.querySwapUsed(),
                    is(aMapWithSize(PagingPercentProperty.values().length)));
            assertThat("Failed PDH querySwapUsed",
                    PerfCounterQuery.queryValuesFromPDH(PagingPercentProperty.class, PAGING_FILE),
                    is(aMapWithSize(PagingPercentProperty.values().length)));
            assertThat("Failed WMI querySwapUsed",
                    PerfCounterQuery.queryValuesFromWMI(PagingPercentProperty.class,
                            WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE),
                    is(aMapWithSize(PagingPercentProperty.values().length)));
        }
    }

    @Test
    void testQueryDiskCounters() {
        if (Platform.isWindows()) {
            testWildcardCounters("Combined DiskCounters", PhysicalDisk.queryDiskCounters());
            testWildcardCounters("PDH DiskCounters",
                    PerfCounterWildcardQuery.queryInstancesAndValuesFromPDH(PhysicalDiskProperty.class, PHYSICAL_DISK));
            testWildcardCounters("WMI DiskCounters", PerfCounterWildcardQuery.queryInstancesAndValuesFromWMI(
                    PhysicalDiskProperty.class, WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL));
        }
    }

    @Test
    void testQueryProcessCounters() {
        if (Platform.isWindows()) {
            testWildcardCounters("Combined ProcessCounters", ProcessInformation.queryProcessCounters());
            testWildcardCounters("PDH ProcessCounters",
                    PerfCounterWildcardQuery.queryInstancesAndValuesFromPDH(ProcessPerformanceProperty.class, PROCESS));
            testWildcardCounters("WMI ProcessCounters", PerfCounterWildcardQuery.queryInstancesAndValuesFromWMI(
                    ProcessPerformanceProperty.class, WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL));
        }
    }

    @Test
    void testQueryHandles() {
        if (Platform.isWindows()) {
            testWildcardCounters("Combined Handles", ProcessInformation.queryHandles());
            testWildcardCounters("PDH Handles",
                    PerfCounterWildcardQuery.queryInstancesAndValuesFromPDH(HandleCountProperty.class, PROCESS));
            testWildcardCounters("WMI Handles", PerfCounterWildcardQuery
                    .queryInstancesAndValuesFromWMI(HandleCountProperty.class, WIN32_PERFPROC_PROCESS));
        }
    }

    @Test
    void testQueryProcessorCounters() {
        if (Platform.isWindows()) {
            testWildcardCounters("Combined ProcessorCounters", ProcessorInformation.queryProcessorCounters());
            testWildcardCounters("PDH ProcessorCounters", PerfCounterWildcardQuery
                    .queryInstancesAndValuesFromPDH(ProcessorTickCountProperty.class, PROCESSOR));
            testWildcardCounters("WMI ProcessorCounters", PerfCounterWildcardQuery.queryInstancesAndValuesFromWMI(
                    ProcessorTickCountProperty.class, WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL));
            if (VersionHelpers.IsWindows7OrGreater()) {
                testWildcardCounters("PDH ProcessorCounters", PerfCounterWildcardQuery
                        .queryInstancesAndValuesFromPDH(ProcessorTickCountProperty.class, PROCESSOR_INFORMATION));
                testWildcardCounters("WMI ProcessorCounters",
                        PerfCounterWildcardQuery.queryInstancesAndValuesFromWMI(ProcessorTickCountProperty.class,
                                WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL));
            }
        }
    }

    @Test
    void testQueryInterruptCounters() {
        if (Platform.isWindows()) {
            assertThat("Failed Combined queryInterruptCounters", ProcessorInformation.queryInterruptCounters(),
                    is(aMapWithSize(InterruptsProperty.values().length)));
            assertThat("Failed PDH queryInterruptCounters",
                    PerfCounterQuery.queryValuesFromPDH(InterruptsProperty.class, PROCESSOR),
                    is(aMapWithSize(InterruptsProperty.values().length)));
            assertThat("Failed WMI queryInterruptCounters",
                    PerfCounterQuery.queryValuesFromWMI(InterruptsProperty.class,
                            WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL),
                    is(aMapWithSize(InterruptsProperty.values().length)));
        }
    }

    @Test
    void testQueryFrequencyCounters() {
        if (Platform.isWindows()) {
            testWildcardCounters("Combined FrequencyCounters", ProcessorInformation.queryFrequencyCounters());
            testWildcardCounters("PDH FrequencyCounters", PerfCounterWildcardQuery
                    .queryInstancesAndValuesFromPDH(ProcessorFrequencyProperty.class, PROCESSOR_INFORMATION));
            testWildcardCounters("WMI FrequencyCounters",
                    PerfCounterWildcardQuery.queryInstancesAndValuesFromWMI(ProcessorFrequencyProperty.class,
                            WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL));
        }
    }

    @Test
    void testQueryContextSwitchCounters() {
        if (Platform.isWindows()) {
            assertThat("Failed Combined queryContextSwitchCounters", SystemInformation.queryContextSwitchCounters(),
                    is(aMapWithSize(ContextSwitchProperty.values().length)));
            assertThat("Failed PDH queryInterruptCounters",
                    PerfCounterQuery.queryValuesFromPDH(ContextSwitchProperty.class, SYSTEM),
                    is(aMapWithSize(ContextSwitchProperty.values().length)));
            assertThat("Failed WMI queryInterruptCounters",
                    PerfCounterQuery.queryValuesFromWMI(ContextSwitchProperty.class,
                            WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM),
                    is(aMapWithSize(ContextSwitchProperty.values().length)));
        }
    }

    @Test
    void testQueryThreadCounters() {
        if (Platform.isWindows()) {
            testWildcardCounters("Combined ThreadCounters", ThreadInformation.queryThreadCounters());
            testWildcardCounters("PDH ThreadCounters",
                    PerfCounterWildcardQuery.queryInstancesAndValuesFromPDH(ThreadPerformanceProperty.class, THREAD));
            testWildcardCounters("WMI ThreadCounters", PerfCounterWildcardQuery.queryInstancesAndValuesFromWMI(
                    ThreadPerformanceProperty.class, WIN32_PERF_RAW_DATA_PERF_PROC_THREAD));
        }
    }

    private <T extends PdhCounterWildcardProperty> void testWildcardCounters(String s,
            Pair<List<String>, Map<T, List<Long>>> counterData) {
        int instanceCount = counterData.getA().size();
        Map<T, List<Long>> counters = counterData.getB();
        for (List<Long> counter : counters.values()) {
            assertThat("Failed " + s + " instance count", counter.size(), is(instanceCount));
        }

        @SuppressWarnings("unchecked")
        int expectedSize = ((Class<T>) counters.keySet().iterator().next().getClass()).getEnumConstants().length - 1;
        assertThat("Failed " + s + " map size", counters, is(aMapWithSize(expectedSize)));
    }
}
