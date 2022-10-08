/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.PsInfo;
import oshi.jna.platform.unix.AixLibc.AixLwpsInfo;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;

/**
 * OSThread implementation
 */
@ThreadSafe
public class AixOSThread extends AbstractOSThread {

    private int threadId;
    private OSProcess.State state = OSProcess.State.INVALID;
    private long startMemoryAddress;
    private long contextSwitches;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private int priority;

    public AixOSThread(int pid, int tid) {
        super(pid);
        this.threadId = tid;
        updateAttributes();
    }

    @Override
    public int getThreadId() {
        return this.threadId;
    }

    @Override
    public OSProcess.State getState() {
        return this.state;
    }

    @Override
    public long getStartMemoryAddress() {
        return this.startMemoryAddress;
    }

    @Override
    public long getContextSwitches() {
        return this.contextSwitches;
    }

    @Override
    public long getKernelTime() {
        return this.kernelTime;
    }

    @Override
    public long getUserTime() {
        return this.userTime;
    }

    @Override
    public long getUpTime() {
        return this.upTime;
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public boolean updateAttributes() {
        AixLwpsInfo lwpsinfo = PsInfo.queryLwpsInfo(getOwningProcessId(), getThreadId());
        if (lwpsinfo == null) {
            this.state = OSProcess.State.INVALID;
            return false;
        }
        this.threadId = (int) lwpsinfo.pr_lwpid; // 64 bit storage but always 32 bit
        this.startMemoryAddress = lwpsinfo.pr_addr;
        this.state = AixOSProcess.getStateFromOutput((char) lwpsinfo.pr_sname);
        this.priority = lwpsinfo.pr_pri;
        return true;
    }
}
