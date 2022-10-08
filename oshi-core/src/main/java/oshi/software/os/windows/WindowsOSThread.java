/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.NEW;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.SUSPENDED;
import static oshi.software.os.OSProcess.State.WAITING;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.registry.ThreadPerformanceData;
import oshi.driver.windows.registry.ThreadPerformanceData.PerfCounterBlock;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess.State;

/**
 * OSThread implementation
 */
@ThreadSafe
public class WindowsOSThread extends AbstractOSThread {

    private final int threadId;
    private String name;
    private State state;
    private long startMemoryAddress;
    private long contextSwitches;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private int priority;

    public WindowsOSThread(int pid, int tid, String procName, PerfCounterBlock pcb) {
        super(pid);
        this.threadId = tid;
        updateAttributes(procName, pcb);
    }

    @Override
    public int getThreadId() {
        return threadId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public long getStartMemoryAddress() {
        return startMemoryAddress;
    }

    @Override
    public long getContextSwitches() {
        return contextSwitches;
    }

    @Override
    public long getKernelTime() {
        return kernelTime;
    }

    @Override
    public long getUserTime() {
        return userTime;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getUpTime() {
        return upTime;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public boolean updateAttributes() {
        Set<Integer> pids = Collections.singleton(getOwningProcessId());
        // Get data from the registry if possible
        Map<Integer, ThreadPerformanceData.PerfCounterBlock> threads = ThreadPerformanceData
                .buildThreadMapFromRegistry(pids);
        // otherwise performance counters with WMI backup
        if (threads == null) {
            threads = ThreadPerformanceData.buildThreadMapFromPerfCounters(pids);
        }
        return updateAttributes(this.name.split("/")[0], threads.get(getThreadId()));
    }

    private boolean updateAttributes(String procName, PerfCounterBlock pcb) {
        if (pcb == null) {
            this.state = INVALID;
            return false;
        } else if (pcb.getName().contains("/") || procName.isEmpty()) {
            name = pcb.getName();
        } else {
            this.name = procName + "/" + pcb.getName();
        }
        if (pcb.getThreadWaitReason() == 5) {
            state = SUSPENDED;
        } else {
            switch (pcb.getThreadState()) {
            case 0:
                state = NEW;
                break;
            case 2:
            case 3:
                state = RUNNING;
                break;
            case 4:
                state = STOPPED;
                break;
            case 5:
                state = SLEEPING;
                break;
            case 1:
            case 6:
                state = WAITING;
                break;
            case 7:
            default:
                state = OTHER;
            }
        }
        startMemoryAddress = pcb.getStartAddress();
        contextSwitches = pcb.getContextSwitches();
        kernelTime = pcb.getKernelTime();
        userTime = pcb.getUserTime();
        startTime = pcb.getStartTime();
        upTime = System.currentTimeMillis() - pcb.getStartTime();
        priority = pcb.getPriority();
        return true;
    }
}
