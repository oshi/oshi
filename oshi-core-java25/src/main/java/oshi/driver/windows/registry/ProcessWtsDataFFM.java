/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.WtsInfo;
import oshi.driver.common.windows.wmi.Win32Process.ProcessXPProperty;
import oshi.driver.windows.wmi.Win32ProcessFFM;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiUtilFFM;

/**
 * Utility to read process data from WMI as a substitute for WTS native calls.
 */
@ThreadSafe
public final class ProcessWtsDataFFM {

    private ProcessWtsDataFFM() {
    }

    /**
     * Query WMI for process performance counters.
     *
     * @param pids An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @return A map with Process ID as the key and a {@link WtsInfo} object populated with data.
     */
    public static Map<Integer, WtsInfo> queryProcessWtsMap(Collection<Integer> pids) {
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
