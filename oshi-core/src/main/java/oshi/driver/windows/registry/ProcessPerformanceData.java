/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.windows.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.tuples.Triplet;

/**
 * Utility to read process data from HKEY_PERFORMANCE_DATA information.
 */
@ThreadSafe
public final class ProcessPerformanceData {

    private static final String PROCESS = "Process";

    private ProcessPerformanceData() {
    }

    /**
     * Query the registry for process performance counters
     *
     * @param os
     *            The OperatingSystem object
     * @param pids
     *            An optional collection of process IDs to filter the list to. May
     *            be null for no filtering.
     * @return A map with Process ID as the key and an OSProcess object populated
     *         with performance counter information if successful, or null
     *         otherwise.
     */
    public static Map<Integer, OSProcess> buildProcessMapFromRegistry(OperatingSystem os, Collection<Integer> pids) {
        // Grab the data from the registry.
        Triplet<List<Map<ProcessPerformanceProperty, Object>>, Long, Long> processData = HkeyPerformanceDataUtil
                .readPerfDataFromRegistry(PROCESS, ProcessPerformanceProperty.class);
        if (processData == null) {
            return null;
        }
        List<Map<ProcessPerformanceProperty, Object>> processInstanceMaps = processData.getA();
        long perfTime100nSec = processData.getB(); // 1601
        long now = processData.getC(); // 1970 epoch

        // Create a map and fill it
        Map<Integer, OSProcess> processMap = new HashMap<>();
        // Iterate instances.
        for (Map<ProcessPerformanceProperty, Object> processInstanceMap : processInstanceMaps) {
            int pid = ((Integer) processInstanceMap.get(ProcessPerformanceProperty.PROCESSID)).intValue();
            String name = (String) processInstanceMap.get(ProcessPerformanceProperty.NAME);
            if ((pids == null || pids.contains(pid)) && !"_Total".equals(name)) {
                OSProcess proc = new OSProcess(os);
                proc.setProcessID(pid);
                proc.setName(name);
                long upTime = (perfTime100nSec - (Long) processInstanceMap.get(ProcessPerformanceProperty.CREATIONDATE))
                        / 10_000L;
                proc.setUpTime(upTime < 1L ? 1L : upTime);
                proc.setStartTime(now - upTime);
                proc.setBytesRead((Long) processInstanceMap.get(ProcessPerformanceProperty.READTRANSFERCOUNT));
                proc.setBytesWritten((Long) processInstanceMap.get(ProcessPerformanceProperty.WRITETRANSFERCOUNT));
                proc.setResidentSetSize((Long) processInstanceMap.get(ProcessPerformanceProperty.PRIVATEPAGECOUNT));
                proc.setParentProcessID((Integer) processInstanceMap.get(ProcessPerformanceProperty.PARENTPROCESSID));
                proc.setPriority((Integer) processInstanceMap.get(ProcessPerformanceProperty.PRIORITY));

                processMap.put(pid, proc);
            }
        }
        return processMap;
    }
}
