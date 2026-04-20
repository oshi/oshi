/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.NEW;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.SUSPENDED;
import static oshi.software.os.OSProcess.State.WAITING;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess.State;

/**
 * Common base class for Windows OS thread implementations.
 */
@ThreadSafe
public abstract class WindowsOSThread extends AbstractOSThread {

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

    protected WindowsOSThread(int pid, int tid, String procName, ThreadPerfCounterBlock pcb) {
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

    /**
     * Returns the process name prefix used for thread name construction.
     *
     * @return the process name (portion before "/" in the thread name)
     */
    protected String getProcName() {
        return this.name == null ? "" : this.name.split("/")[0];
    }

    /**
     * Updates thread attributes from a performance counter block.
     *
     * @param procName the owning process name
     * @param pcb      the thread performance counter block, or null if unavailable
     * @return true if the thread is valid after the update
     */
    protected boolean updateAttributes(String procName, ThreadPerfCounterBlock pcb) {
        if (pcb == null) {
            this.state = INVALID;
            return false;
        } else if (pcb.getName().contains("/") || procName == null || procName.isEmpty()) {
            this.name = pcb.getName();
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
