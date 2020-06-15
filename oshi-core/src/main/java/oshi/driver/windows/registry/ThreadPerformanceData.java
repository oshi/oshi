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

import com.sun.jna.platform.win32.WinBase;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.perfmon.ThreadInformation;
import oshi.driver.windows.perfmon.ThreadInformation.ThreadPerformanceProperty;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to read thread data from HKEY_PERFORMANCE_DATA information with
 * backup from Performance Counters or WMI
 */
@ThreadSafe
public final class ThreadPerformanceData {

    private static final String THREAD = "Thread";

    private ThreadPerformanceData() {
    }

    /**
     * Query the registry for thread performance counters
     *
     * @param pids
     *            An optional collection of thread IDs to filter the list to. May be
     *            null for no filtering.
     * @return A map with Thread ID as the key and a {@link PerfCounterBlock} object
     *         populated with performance counter information if successful, or null
     *         otherwise.
     */
    public static Map<Integer, PerfCounterBlock> buildThreadMapFromRegistry(Collection<Integer> pids) {
        // Grab the data from the registry.
        Triplet<List<Map<ThreadPerformanceProperty, Object>>, Long, Long> threadData = HkeyPerformanceDataUtil
                .readPerfDataFromRegistry(THREAD, ThreadPerformanceProperty.class);
        if (threadData == null) {
            return null;
        }
        List<Map<ThreadPerformanceProperty, Object>> threadInstanceMaps = threadData.getA();
        long perfTime100nSec = threadData.getB(); // 1601
        // ignore threadData.getC() -- 1970 epoch

        // Create a map and fill it
        Map<Integer, PerfCounterBlock> threadMap = new HashMap<>();
        // Iterate instances.
        for (Map<ThreadPerformanceProperty, Object> threadInstanceMap : threadInstanceMaps) {
            int pid = ((Integer) threadInstanceMap.get(ThreadPerformanceProperty.PROCESSHANDLE)).intValue();
            if ((pids == null || pids.contains(pid)) && pid > 0) {
                int tid = ((Integer) threadInstanceMap.get(ThreadPerformanceProperty.HANDLE)).intValue();
                String name = String.format("0x%x", threadInstanceMap.get(ThreadPerformanceProperty.STARTADDRESS));
                long upTime = (perfTime100nSec - (Long) threadInstanceMap.get(ThreadPerformanceProperty.ELAPSEDTIME))
                        / 10_000L;
                if (upTime < 1L) {
                    upTime = 1L;
                }
                long user = ((Long) threadInstanceMap.get(ThreadPerformanceProperty.USERMODETIME)).longValue();
                long kernel = ((Long) threadInstanceMap.get(ThreadPerformanceProperty.KERNELMODETIME)).longValue();
                int priority = ((Integer) threadInstanceMap.get(ThreadPerformanceProperty.PRIORITY)).intValue();
                int threadState = ((Integer) threadInstanceMap.get(ThreadPerformanceProperty.THREADSTATE)).intValue();
                threadMap.put(tid, new PerfCounterBlock(name, tid, pid, upTime, user, kernel, priority, threadState));
            }
        }
        return threadMap;
    }

    /**
     * Query PerfMon for thread performance counters
     *
     * @param pids
     *            An optional collection of process IDs to filter the list to. May
     *            be null for no filtering.
     * @return A map with Thread ID as the key and a {@link PerfCounterBlock} object
     *         populated with performance counter information.
     */
    public static Map<Integer, PerfCounterBlock> buildThreadMapFromPerfCounters(Collection<Integer> pids) {
        Map<Integer, PerfCounterBlock> threadMap = new HashMap<>();
        Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> instanceValues = ThreadInformation
                .queryThreadCounters();
        long now = System.currentTimeMillis(); // 1970 epoch
        List<String> instances = instanceValues.getA();
        Map<ThreadPerformanceProperty, List<Long>> valueMap = instanceValues.getB();
        List<Long> tidList = valueMap.get(ThreadPerformanceProperty.HANDLE);
        List<Long> pidList = valueMap.get(ThreadPerformanceProperty.PROCESSHANDLE);
        List<Long> userList = valueMap.get(ThreadPerformanceProperty.USERMODETIME);
        List<Long> kernelList = valueMap.get(ThreadPerformanceProperty.KERNELMODETIME);
        List<Long> upTimeList = valueMap.get(ThreadPerformanceProperty.ELAPSEDTIME);
        List<Long> priorityList = valueMap.get(ThreadPerformanceProperty.PRIORITY);
        List<Long> stateList = valueMap.get(ThreadPerformanceProperty.THREADSTATE);

        for (int inst = 0; inst < instances.size(); inst++) {
            int pid = pidList.get(inst).intValue();
            if (pids == null || pids.contains(pid)) {
                int tid = tidList.get(inst).intValue();
                String name = "" + inst;
                long upTime = upTimeList.get(inst);
                if (upTime > now) {
                    upTime = WinBase.FILETIME.filetimeToDate((int) (upTime >> 32), (int) (upTime & 0xffffffffL))
                            .getTime();
                }
                long user = userList.get(inst);
                long kernel = kernelList.get(inst);
                int priority = priorityList.get(inst).intValue();
                int threadState = stateList.get(inst).intValue();
                // if creation time value is less than current millis, it's in 1970 epoch,
                // otherwise it's 1601 epoch and we must convert
                threadMap.put(pid, new PerfCounterBlock(name, tid, pid, upTime, user, kernel, priority, threadState));
            }
        }
        return threadMap;

    }

    /**
     * Class to encapsulate data from the registry performance counter block
     */
    @Immutable
    public static class PerfCounterBlock {
        private final String name;
        private final int threadID;
        private final int owningProcessID;
        private final long upTime;
        private final long userTime;
        private final long kernelTime;
        private final int priority;
        private final int threadState;

        public PerfCounterBlock(String name, int threadID, int owningProcessID, long upTime, long userTime,
                long kernelTime, int priority, int threadState) {
            this.name = name;
            this.threadID = threadID;
            this.owningProcessID = owningProcessID;
            this.upTime = upTime;
            this.userTime = userTime;
            this.kernelTime = kernelTime;
            this.priority = priority;
            this.threadState = threadState;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the threadID
         */
        public int getThreadID() {
            return threadID;
        }

        /**
         * @return the owningProcessID
         */
        public int getOwningProcessID() {
            return owningProcessID;
        }

        /**
         * @return the upTime
         */
        public long getUpTime() {
            return upTime;
        }

        /**
         * @return the userTime
         */
        public long getUserTime() {
            return userTime;
        }

        /**
         * @return the kernelTime
         */
        public long getKernelTime() {
            return kernelTime;
        }

        /**
         * @return the priority
         */
        public int getPriority() {
            return priority;
        }

        /**
         * @return the threadState
         */
        public int getThreadState() {
            return threadState;
        }
    }
}
