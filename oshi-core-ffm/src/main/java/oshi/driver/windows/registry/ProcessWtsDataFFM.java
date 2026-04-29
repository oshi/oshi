/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;
import static oshi.ffm.windows.Wtsapi32FFM.PROCESS_INFO_EX_HANDLE_COUNT;
import static oshi.ffm.windows.Wtsapi32FFM.PROCESS_INFO_EX_KERNEL_TIME;
import static oshi.ffm.windows.Wtsapi32FFM.PROCESS_INFO_EX_NUMBER_OF_THREADS;
import static oshi.ffm.windows.Wtsapi32FFM.PROCESS_INFO_EX_PAGEFILE_USAGE;
import static oshi.ffm.windows.Wtsapi32FFM.PROCESS_INFO_EX_PROCESS_ID;
import static oshi.ffm.windows.Wtsapi32FFM.PROCESS_INFO_EX_PROCESS_NAME;
import static oshi.ffm.windows.Wtsapi32FFM.PROCESS_INFO_EX_SIZE;
import static oshi.ffm.windows.Wtsapi32FFM.PROCESS_INFO_EX_USER_TIME;
import static oshi.ffm.windows.Wtsapi32FFM.WTS_ANY_SESSION;
import static oshi.ffm.windows.Wtsapi32FFM.WTS_CURRENT_SERVER_HANDLE;
import static oshi.ffm.windows.Wtsapi32FFM.WTS_PROCESS_INFO_LEVEL_1;
import static oshi.ffm.windows.Wtsapi32FFM.WTS_TYPE_PROCESS_INFO_LEVEL_1;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.WtsInfo;
import oshi.driver.common.windows.wmi.Win32Process.ProcessXPProperty;
import oshi.driver.windows.wmi.Win32ProcessFFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.VersionHelpersFFM;
import oshi.ffm.windows.Wtsapi32FFM;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiUtilFFM;

/**
 * Utility to read process data from WTS native calls with backup from WMI.
 */
@ThreadSafe
public final class ProcessWtsDataFFM {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessWtsDataFFM.class);

    private static final boolean IS_WINDOWS7_OR_GREATER = VersionHelpersFFM.IsWindows7OrGreater();

    private ProcessWtsDataFFM() {
    }

    /**
     * Query process information using native WTS calls (Win7+) or WMI as fallback.
     *
     * @param pids An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @return A map with Process ID as the key and a {@link WtsInfo} object populated with data.
     */
    public static Map<Integer, WtsInfo> queryProcessWtsMap(Collection<Integer> pids) {
        if (IS_WINDOWS7_OR_GREATER) {
            return queryProcessWtsMapFromWTS(pids);
        }
        return queryProcessWtsMapFromWMI(pids);
    }

    private static Map<Integer, WtsInfo> queryProcessWtsMapFromWTS(Collection<Integer> pids) {
        Set<Integer> pidSet = pids != null ? new HashSet<>(pids) : null;
        Map<Integer, WtsInfo> wtsMap = new HashMap<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pLevel = arena.allocate(JAVA_INT);
            pLevel.set(JAVA_INT, 0, WTS_PROCESS_INFO_LEVEL_1);
            MemorySegment ppProcessInfo = arena.allocate(ADDRESS);
            MemorySegment pCount = arena.allocate(JAVA_INT);

            if (!Wtsapi32FFM.WTSEnumerateProcessesEx(WTS_CURRENT_SERVER_HANDLE, pLevel, WTS_ANY_SESSION, ppProcessInfo,
                    pCount)) {
                LOG.error("Failed to enumerate Processes. Error code: {}", Kernel32FFM.GetLastError().orElse(-1));
                return wtsMap;
            }

            int count = pCount.get(JAVA_INT, 0);
            MemorySegment pProcessInfo = ppProcessInfo.get(ADDRESS, 0).reinterpret(PROCESS_INFO_EX_SIZE * count);
            try {
                for (int i = 0; i < count; i++) {
                    long base = i * PROCESS_INFO_EX_SIZE;
                    int pid = pProcessInfo.get(JAVA_INT, base + PROCESS_INFO_EX_PROCESS_ID);
                    if (pidSet != null && !pidSet.contains(pid)) {
                        continue;
                    }
                    MemorySegment pName = pProcessInfo.get(ADDRESS, base + PROCESS_INFO_EX_PROCESS_NAME);
                    String name = pName.equals(MemorySegment.NULL) ? "" : readWideString(pName.reinterpret(512));
                    int threads = pProcessInfo.get(JAVA_INT, base + PROCESS_INFO_EX_NUMBER_OF_THREADS);
                    long pagefileUsage = pProcessInfo.get(JAVA_INT, base + PROCESS_INFO_EX_PAGEFILE_USAGE)
                            & 0xffff_ffffL;
                    long kernelTime = pProcessInfo.get(JAVA_LONG, base + PROCESS_INFO_EX_KERNEL_TIME) / 10_000L;
                    long userTime = pProcessInfo.get(JAVA_LONG, base + PROCESS_INFO_EX_USER_TIME) / 10_000L;
                    int handles = pProcessInfo.get(JAVA_INT, base + PROCESS_INFO_EX_HANDLE_COUNT);
                    wtsMap.put(pid, new WtsInfo(name, "", threads, pagefileUsage, kernelTime, userTime, handles));
                }
            } finally {
                if (!Wtsapi32FFM.WTSFreeMemoryEx(WTS_TYPE_PROCESS_INFO_LEVEL_1, pProcessInfo, count)) {
                    LOG.warn("Failed to Free Memory for Processes. Error code: {}",
                            Kernel32FFM.GetLastError().orElse(-1));
                }
            }
        } catch (Throwable t) {
            LOG.error("Failed to enumerate Processes via WTS", t);
        }
        return wtsMap;
    }

    private static Map<Integer, WtsInfo> queryProcessWtsMapFromWMI(Collection<Integer> pids) {
        Map<Integer, WtsInfo> wtsMap = new HashMap<>();
        WmiResult<ProcessXPProperty> processWmiResult = Win32ProcessFFM.queryProcesses(pids);
        for (int i = 0; i < processWmiResult.getResultCount(); i++) {
            wtsMap.put(WmiUtilFFM.getUint32(processWmiResult, ProcessXPProperty.PROCESSID, i), new WtsInfo(
                    WmiUtilFFM.getString(processWmiResult, ProcessXPProperty.NAME, i),
                    WmiUtilFFM.getString(processWmiResult, ProcessXPProperty.EXECUTABLEPATH, i),
                    WmiUtilFFM.getUint32(processWmiResult, ProcessXPProperty.THREADCOUNT, i),
                    // WMI Pagefile usage is in KB
                    1024 * (WmiUtilFFM.getUint32(processWmiResult, ProcessXPProperty.PAGEFILEUSAGE, i) & 0xffff_ffffL),
                    WmiUtilFFM.getUint64(processWmiResult, ProcessXPProperty.KERNELMODETIME, i) / 10_000L,
                    WmiUtilFFM.getUint64(processWmiResult, ProcessXPProperty.USERMODETIME, i) / 10_000L,
                    WmiUtilFFM.getUint32(processWmiResult, ProcessXPProperty.HANDLECOUNT, i)));
        }
        return wtsMap;
    }
}
