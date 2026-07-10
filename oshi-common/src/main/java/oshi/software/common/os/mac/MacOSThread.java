/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess.State;

/**
 * OSThread implementation
 */
@ThreadSafe
public class MacOSThread extends AbstractOSThread {

    /**
     * Creates a MacOSThread with full parameters.
     *
     * @param pid        the process ID
     * @param threadId   the thread ID
     * @param state      the thread state
     * @param kernelTime kernel time
     * @param userTime   user time
     * @param startTime  start time
     * @param upTime     up time
     * @param priority   the priority
     */
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

    /**
     * Creates a MacOSThread with just a process ID.
     *
     * @param processId the process ID
     */
    public MacOSThread(int processId) {
        this(processId, 0, State.INVALID, 0L, 0L, 0L, 0L, 0);
    }

}
