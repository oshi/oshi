/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Wtsapi32;
import com.sun.jna.platform.win32.Wtsapi32.WTS_PROCESS_INFO_EX;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.wmi.Win32Process;
import oshi.driver.windows.wmi.Win32Process.ProcessXPProperty;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.util.platform.windows.WmiUtil;

/**
 * Utility to read process data from HKEY_PERFORMANCE_DATA information with backup from Performance Counters or WMI
 */
@ThreadSafe
public final class ProcessWtsData {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessWtsData.class);

    private static final boolean IS_WINDOWS7_OR_GREATER = VersionHelpers.IsWindows7OrGreater();

    private ProcessWtsData() {
    }

    /**
     * Query the registry for process performance counters
     *
     * @param pids An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @return A map with Process ID as the key and a {@link WtsInfo} object populated with data.
     */
    public static Map<Integer, WtsInfo> queryProcessWtsMap(Collection<Integer> pids) {
        if (IS_WINDOWS7_OR_GREATER) {
            // Get processes from WTS
            return queryProcessWtsMapFromWTS(pids);
        }
        // Pre-Win7 we can't use WTSEnumerateProcessesEx so we'll grab the
        // same info from WMI and fake the array
        return queryProcessWtsMapFromPerfMon(pids);
    }

    private static Map<Integer, WtsInfo> queryProcessWtsMapFromWTS(Collection<Integer> pids) {
        Map<Integer, WtsInfo> wtsMap = new HashMap<>();
        try (CloseableIntByReference pCount = new CloseableIntByReference(0);
                CloseablePointerByReference ppProcessInfo = new CloseablePointerByReference();
                CloseableIntByReference infoLevel1 = new CloseableIntByReference(Wtsapi32.WTS_PROCESS_INFO_LEVEL_1)) {
            if (!Wtsapi32.INSTANCE.WTSEnumerateProcessesEx(Wtsapi32.WTS_CURRENT_SERVER_HANDLE, infoLevel1,
                    Wtsapi32.WTS_ANY_SESSION, ppProcessInfo, pCount)) {
                LOG.error("Failed to enumerate Processes. Error code: {}", Kernel32.INSTANCE.GetLastError());
                return wtsMap;
            }
            // extract the pointed-to pointer and create array
            Pointer pProcessInfo = ppProcessInfo.getValue();
            final WTS_PROCESS_INFO_EX processInfoRef = new WTS_PROCESS_INFO_EX(pProcessInfo);
            WTS_PROCESS_INFO_EX[] processInfo = (WTS_PROCESS_INFO_EX[]) processInfoRef.toArray(pCount.getValue());
            for (WTS_PROCESS_INFO_EX info : processInfo) {
                if (pids == null || pids.contains(info.ProcessId)) {
                    wtsMap.put(info.ProcessId,
                            new WtsInfo(info.pProcessName, "", info.NumberOfThreads, info.PagefileUsage & 0xffff_ffffL,
                                    info.KernelTime.getValue() / 10_000L, info.UserTime.getValue() / 10_000,
                                    info.HandleCount));
                }
            }
            // Clean up memory
            if (!Wtsapi32.INSTANCE.WTSFreeMemoryEx(Wtsapi32.WTS_PROCESS_INFO_LEVEL_1, pProcessInfo,
                    pCount.getValue())) {
                LOG.warn("Failed to Free Memory for Processes. Error code: {}", Kernel32.INSTANCE.GetLastError());
            }
        }
        return wtsMap;
    }

    private static Map<Integer, WtsInfo> queryProcessWtsMapFromPerfMon(Collection<Integer> pids) {
        Map<Integer, WtsInfo> wtsMap = new HashMap<>();
        WmiResult<ProcessXPProperty> processWmiResult = Win32Process.queryProcesses(pids);
        for (int i = 0; i < processWmiResult.getResultCount(); i++) {
            wtsMap.put(WmiUtil.getUint32(processWmiResult, ProcessXPProperty.PROCESSID, i), new WtsInfo(
                    WmiUtil.getString(processWmiResult, ProcessXPProperty.NAME, i),
                    WmiUtil.getString(processWmiResult, ProcessXPProperty.EXECUTABLEPATH, i),
                    WmiUtil.getUint32(processWmiResult, ProcessXPProperty.THREADCOUNT, i),
                    // WMI Pagefile usage is in KB
                    1024 * (WmiUtil.getUint32(processWmiResult, ProcessXPProperty.PAGEFILEUSAGE, i) & 0xffff_ffffL),
                    WmiUtil.getUint64(processWmiResult, ProcessXPProperty.KERNELMODETIME, i) / 10_000L,
                    WmiUtil.getUint64(processWmiResult, ProcessXPProperty.USERMODETIME, i) / 10_000L,
                    WmiUtil.getUint32(processWmiResult, ProcessXPProperty.HANDLECOUNT, i)));
        }
        return wtsMap;
    }

    /**
     * Class to encapsulate data from WTS Process Info
     */
    @Immutable
    public static class WtsInfo {
        private final String name;
        private final String path;
        private final int threadCount;
        private final long virtualSize;
        private final long kernelTime;
        private final long userTime;
        private final long openFiles;

        public WtsInfo(String name, String path, int threadCount, long virtualSize, long kernelTime, long userTime,
                long openFiles) {
            this.name = name;
            this.path = path;
            this.threadCount = threadCount;
            this.virtualSize = virtualSize;
            this.kernelTime = kernelTime;
            this.userTime = userTime;
            this.openFiles = openFiles;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /**
         * @return the threadCount
         */
        public int getThreadCount() {
            return threadCount;
        }

        /**
         * @return the virtualSize
         */
        public long getVirtualSize() {
            return virtualSize;
        }

        /**
         * @return the kernelTime
         */
        public long getKernelTime() {
            return kernelTime;
        }

        /**
         * @return the userTime
         */
        public long getUserTime() {
            return userTime;
        }

        /**
         * @return the openFiles
         */
        public long getOpenFiles() {
            return openFiles;
        }
    }
}
