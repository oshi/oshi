/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESS;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.util.platform.windows.PerfCounterQueryFFM;

/**
 * Utility to query Process Information performance counter using FFM.
 */
@ThreadSafe
public final class ProcessInformationFFM {

    private ProcessInformationFFM() {
    }

    /**
     * Returns handle counters.
     *
     * @return Handle count for the _Total instance.
     */
    public static Map<HandleCountProperty, Long> queryHandles() {
        return PerfCounterQueryFFM.queryValues(HandleCountProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL);
    }
}
