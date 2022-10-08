/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.win32.WinBase;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.perfmon.ProcessInformation;
import oshi.driver.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.util.GlobalConfig;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to read process data from HKEY_PERFORMANCE_DATA information with backup from Performance Counters or WMI
 */
@ThreadSafe
public final class ProcessPerformanceData {

    private static final String PROCESS = "Process";
    private static final boolean PERFDATA = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_HKEYPERFDATA, true);

    private ProcessPerformanceData() {
    }

    /**
     * Query the registry for process performance counters
     *
     * @param pids An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @return A map with Process ID as the key and a {@link PerfCounterBlock} object populated with performance counter
     *         information if successful, or null otherwise.
     */
    public static Map<Integer, PerfCounterBlock> buildProcessMapFromRegistry(Collection<Integer> pids) {
        // Grab the data from the registry.
        Triplet<List<Map<ProcessPerformanceProperty, Object>>, Long, Long> processData = null;
        if (PERFDATA) {
            processData = HkeyPerformanceDataUtil.readPerfDataFromRegistry(PROCESS, ProcessPerformanceProperty.class);
        }
        if (processData == null) {
            return null;
        }
        List<Map<ProcessPerformanceProperty, Object>> processInstanceMaps = processData.getA();
        long now = processData.getC(); // 1970 epoch

        // Create a map and fill it
        Map<Integer, PerfCounterBlock> processMap = new HashMap<>();
        // Iterate instances.
        for (Map<ProcessPerformanceProperty, Object> processInstanceMap : processInstanceMaps) {
            int pid = ((Integer) processInstanceMap.get(ProcessPerformanceProperty.IDPROCESS)).intValue();
            String name = (String) processInstanceMap.get(ProcessPerformanceProperty.NAME);
            if ((pids == null || pids.contains(pid)) && !"_Total".equals(name)) {
                // Field name is elapsed time but the value is the process start time
                long ctime = (Long) processInstanceMap.get(ProcessPerformanceProperty.ELAPSEDTIME);
                // if creation time value is less than current millis, it's in 1970 epoch,
                // otherwise it's 1601 epoch and we must convert
                if (ctime > now) {
                    ctime = WinBase.FILETIME.filetimeToDate((int) (ctime >> 32), (int) (ctime & 0xffffffffL)).getTime();
                }
                long upTime = now - ctime;
                if (upTime < 1L) {
                    upTime = 1L;
                }
                processMap.put(pid,
                        new PerfCounterBlock(name,
                                (Integer) processInstanceMap.get(ProcessPerformanceProperty.CREATINGPROCESSID),
                                (Integer) processInstanceMap.get(ProcessPerformanceProperty.PRIORITYBASE),
                                (Long) processInstanceMap.get(ProcessPerformanceProperty.PRIVATEBYTES), ctime, upTime,
                                (Long) processInstanceMap.get(ProcessPerformanceProperty.IOREADBYTESPERSEC),
                                (Long) processInstanceMap.get(ProcessPerformanceProperty.IOWRITEBYTESPERSEC),
                                (Integer) processInstanceMap.get(ProcessPerformanceProperty.PAGEFAULTSPERSEC)));
            }
        }
        return processMap;
    }

    /**
     * Query PerfMon for process performance counters
     *
     * @param pids An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @return A map with Process ID as the key and a {@link PerfCounterBlock} object populated with performance counter
     *         information.
     */
    public static Map<Integer, PerfCounterBlock> buildProcessMapFromPerfCounters(Collection<Integer> pids) {
        Map<Integer, PerfCounterBlock> processMap = new HashMap<>();
        Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> instanceValues = ProcessInformation
                .queryProcessCounters();
        long now = System.currentTimeMillis(); // 1970 epoch
        List<String> instances = instanceValues.getA();
        Map<ProcessPerformanceProperty, List<Long>> valueMap = instanceValues.getB();
        List<Long> pidList = valueMap.get(ProcessPerformanceProperty.IDPROCESS);
        List<Long> ppidList = valueMap.get(ProcessPerformanceProperty.CREATINGPROCESSID);
        List<Long> priorityList = valueMap.get(ProcessPerformanceProperty.PRIORITYBASE);
        List<Long> ioReadList = valueMap.get(ProcessPerformanceProperty.IOREADBYTESPERSEC);
        List<Long> ioWriteList = valueMap.get(ProcessPerformanceProperty.IOWRITEBYTESPERSEC);
        List<Long> workingSetSizeList = valueMap.get(ProcessPerformanceProperty.PRIVATEBYTES);
        List<Long> elapsedTimeList = valueMap.get(ProcessPerformanceProperty.ELAPSEDTIME);
        List<Long> pageFaultsList = valueMap.get(ProcessPerformanceProperty.PAGEFAULTSPERSEC);

        for (int inst = 0; inst < instances.size(); inst++) {
            int pid = pidList.get(inst).intValue();
            if (pids == null || pids.contains(pid)) {
                // Field name is elapsed time but the value is the process start time
                long ctime = elapsedTimeList.get(inst);
                // if creation time value is less than current millis, it's in 1970 epoch,
                // otherwise it's 1601 epoch and we must convert
                if (ctime > now) {
                    ctime = WinBase.FILETIME.filetimeToDate((int) (ctime >> 32), (int) (ctime & 0xffffffffL)).getTime();
                }
                long upTime = now - ctime;
                if (upTime < 1L) {
                    upTime = 1L;
                }
                processMap.put(pid,
                        new PerfCounterBlock(instances.get(inst), ppidList.get(inst).intValue(),
                                priorityList.get(inst).intValue(), workingSetSizeList.get(inst), ctime, upTime,
                                ioReadList.get(inst), ioWriteList.get(inst), pageFaultsList.get(inst).intValue()));
            }
        }
        return processMap;
    }

    /**
     * Class to encapsulate data from the registry performance counter block
     */
    @Immutable
    public static class PerfCounterBlock {
        private final String name;
        private final int parentProcessID;
        private final int priority;
        private final long residentSetSize;
        private final long startTime;
        private final long upTime;
        private final long bytesRead;
        private final long bytesWritten;
        private final int pageFaults;

        public PerfCounterBlock(String name, int parentProcessID, int priority, long residentSetSize, long startTime,
                long upTime, long bytesRead, long bytesWritten, int pageFaults) {
            this.name = name;
            this.parentProcessID = parentProcessID;
            this.priority = priority;
            this.residentSetSize = residentSetSize;
            this.startTime = startTime;
            this.upTime = upTime;
            this.bytesRead = bytesRead;
            this.bytesWritten = bytesWritten;
            this.pageFaults = pageFaults;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the parentProcessID
         */
        public int getParentProcessID() {
            return parentProcessID;
        }

        /**
         * @return the priority
         */
        public int getPriority() {
            return priority;
        }

        /**
         * @return the residentSetSize
         */
        public long getResidentSetSize() {
            return residentSetSize;
        }

        /**
         * @return the startTime
         */
        public long getStartTime() {
            return startTime;
        }

        /**
         * @return the upTime
         */
        public long getUpTime() {
            return upTime;
        }

        /**
         * @return the bytesRead
         */
        public long getBytesRead() {
            return bytesRead;
        }

        /**
         * @return the bytesWritten
         */
        public long getBytesWritten() {
            return bytesWritten;
        }

        /**
         * @return the pageFaults
         */
        public long getPageFaults() {
            return pageFaults;
        }
    }
}
