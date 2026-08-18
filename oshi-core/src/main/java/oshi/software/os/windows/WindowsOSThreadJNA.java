/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerformanceData;
import oshi.driver.windows.perfmon.PerfCounterQueryExecutorJNA;

/**
 * JNA-based Windows OS thread implementation.
 */
@ThreadSafe
public class WindowsOSThreadJNA extends oshi.software.common.os.windows.WindowsOSThread {

    public WindowsOSThreadJNA(int pid, int tid, @Nullable String procName, @Nullable ThreadPerfCounterBlock pcb) {
        super(pid, tid, procName, pcb);
    }

    @Override
    public boolean updateAttributes() {
        Set<Integer> pids = Collections.singleton(getOwningProcessId());
        String procName = getProcName();
        Map<Integer, ThreadPerfCounterBlock> threads = ThreadPerformanceData
                .buildThreadMapFromPerfCounters(PerfCounterQueryExecutorJNA.INSTANCE, pids, procName, getThreadId());
        return updateAttributes(procName, threads == null ? null : threads.get(getThreadId()));
    }
}
