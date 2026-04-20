/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.windows.registry.ThreadPerformanceDataFFM;

/**
 * FFM-based Windows OS thread implementation.
 */
@ThreadSafe
public class WindowsOSThreadFFM extends oshi.software.common.os.windows.WindowsOSThread {

    public WindowsOSThreadFFM(int pid, int tid, String procName, ThreadPerfCounterBlock pcb) {
        super(pid, tid, procName, pcb);
    }

    @Override
    public boolean updateAttributes() {
        Set<Integer> pids = Collections.singleton(getOwningProcessId());
        String procName = getProcName();
        Map<Integer, ThreadPerfCounterBlock> threads = ThreadPerformanceDataFFM.buildThreadMapFromPerfCounters(pids,
                procName, getThreadId());
        return updateAttributes(procName, threads == null ? null : threads.get(getThreadId()));
    }
}
