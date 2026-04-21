/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.lang.foreign.MemorySegment;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.WinRegFFM;
import oshi.util.GlobalConfig;
import oshi.util.Util;
import oshi.util.platform.windows.Advapi32UtilFFM;

/**
 * Tests whether performance counters are disabled
 */
@ThreadSafe
public final class PerfmonDisabledFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PerfmonDisabledFFM.class);

    public static final boolean PERF_OS_DISABLED = isDisabled(GlobalConfig.OSHI_OS_WINDOWS_PERFOS_DISABLED, "PerfOS");
    public static final boolean PERF_PROC_DISABLED = isDisabled(GlobalConfig.OSHI_OS_WINDOWS_PERFPROC_DISABLED,
            "PerfProc");
    public static final boolean PERF_DISK_DISABLED = isDisabled(GlobalConfig.OSHI_OS_WINDOWS_PERFDISK_DISABLED,
            "PerfDisk");

    private PerfmonDisabledFFM() {
        throw new AssertionError();
    }

    private static boolean isDisabled(String config, String service) {
        String perfDisabled = GlobalConfig.get(config);
        // If null or empty, check registry
        if (Util.isBlank(perfDisabled)) {
            String key = String.format(Locale.ROOT, "SYSTEM\\CurrentControlSet\\Services\\%s\\Performance", service);
            String value = "Disable Performance Counters";
            MemorySegment hklm = MemorySegment.ofAddress(WinRegFFM.HKEY_LOCAL_MACHINE);
            Object disabled = Advapi32UtilFFM.registryGetValue(hklm, key, value);
            if (disabled instanceof Integer) {
                if ((Integer) disabled > 0) {
                    LOG.warn("{} counters are disabled and won't return data: {}\\{}\\{} > 0.", service,
                            "HKEY_LOCAL_MACHINE", key, value);
                    return true;
                }
            } else if (disabled != null) {
                LOG.warn(
                        "Invalid registry value type detected for {} counters. Should be REG_DWORD. Ignoring: {}\\{}\\{}.",
                        service, "HKEY_LOCAL_MACHINE", key, value);
            }
            return false;
        }
        // If not null or empty, parse as boolean
        return Boolean.parseBoolean(perfDisabled);
    }
}
