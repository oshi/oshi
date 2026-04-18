/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import oshi.annotation.concurrent.Immutable;

/**
 * Encapsulates thread performance data from the registry performance counter block.
 */
@Immutable
public final class ThreadPerfCounterBlock {
    private final String name;
    private final int threadID;
    private final int owningProcessID;
    private final long startTime;
    private final long userTime;
    private final long kernelTime;
    private final int priority;
    private final int threadState;
    private final int threadWaitReason;
    private final long startAddress;
    private final long contextSwitches;

    public ThreadPerfCounterBlock(String name, int threadID, int owningProcessID, long startTime, long userTime,
            long kernelTime, int priority, int threadState, int threadWaitReason, long startAddress,
            long contextSwitches) {
        this.name = name;
        this.threadID = threadID;
        this.owningProcessID = owningProcessID;
        this.startTime = startTime;
        this.userTime = userTime;
        this.kernelTime = kernelTime;
        this.priority = priority;
        this.threadState = threadState;
        this.threadWaitReason = threadWaitReason;
        this.startAddress = startAddress;
        this.contextSwitches = contextSwitches;
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
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
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

    /**
     * @return the threadWaitReason
     */
    public int getThreadWaitReason() {
        return threadWaitReason;
    }

    /**
     * @return the startMemoryAddress
     */
    public long getStartAddress() {
        return startAddress;
    }

    /**
     * @return the contextSwitches
     */
    public long getContextSwitches() {
        return contextSwitches;
    }
}
