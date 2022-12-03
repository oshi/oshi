/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess.State;

/**
 * OSThread implementation
 */
@ThreadSafe
public class MacOSThread extends AbstractOSThread {

    private final int threadId;
    private final State state;
    private final long kernelTime;
    private final long userTime;
    private final long startTime;
    private final long upTime;
    private final int priority;

    public MacOSThread(int pid, int threadId, State state, long kernelTime, long userTime, long startTime, long upTime,
            int priority) {
        super(pid);
        this.threadId = threadId;
        this.state = state;
        this.kernelTime = kernelTime;
        this.userTime = userTime;
        this.startTime = startTime;
        this.upTime = upTime;
        this.priority = priority;
    }

    public MacOSThread(int processId) {
        this(processId, 0, State.INVALID, 0L, 0L, 0L, 0L, 0);
    }

    @Override
    public int getThreadId() {
        return threadId;
    }

    @Override
    public State getState() {
        return state;
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
        return priority;
    }
}
