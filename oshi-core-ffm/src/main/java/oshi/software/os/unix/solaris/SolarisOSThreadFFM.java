/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.driver.unix.solaris.PsInfoFFM;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;
import oshi.util.Util;

@ThreadSafe
public class SolarisOSThreadFFM extends AbstractOSThread {

    private final Supplier<PsInfoFFM.LwpsInfo> lwpsinfo = memoize(this::queryLwpsInfo, defaultExpiration());
    private final Supplier<PsInfoFFM.PrUsage> prusage = memoize(this::queryPrUsage, defaultExpiration());

    private String name;
    private int threadId;
    private OSProcess.State state = OSProcess.State.INVALID;
    private long startMemoryAddress;
    private long contextSwitches;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private int priority;

    public SolarisOSThreadFFM(int pid, int lwpid) {
        super(pid);
        this.threadId = lwpid;
        updateAttributes();
    }

    private PsInfoFFM.LwpsInfo queryLwpsInfo() {
        return PsInfoFFM.queryLwpsInfo(this.getOwningProcessId(), this.getThreadId());
    }

    private PsInfoFFM.PrUsage queryPrUsage() {
        return PsInfoFFM.queryPrUsage(this.getOwningProcessId(), this.getThreadId());
    }

    @Override
    public String getName() {
        return this.name != null ? name : "";
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
        PsInfoFFM.LwpsInfo info = lwpsinfo.get();
        if (info == null) {
            this.state = INVALID;
            return false;
        }
        PsInfoFFM.PrUsage usage = prusage.get();
        long now = System.currentTimeMillis();
        this.state = SolarisOSProcessFFM.getStateFromOutput((char) info.pr_sname);
        this.startTime = info.pr_start.toMillis();
        long elapsedTime = now - this.startTime;
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.kernelTime = 0L;
        this.userTime = info.pr_time.toMillis();
        this.startMemoryAddress = info.pr_addr;
        this.priority = info.pr_pri;
        if (usage != null) {
            this.userTime = usage.pr_utime.toMillis();
            this.kernelTime = usage.pr_stime.toMillis();
            this.contextSwitches = usage.pr_ictx + usage.pr_vctx;
        }
        this.name = PsInfoFFM.bytesToString(info.pr_name);
        if (Util.isBlank(name)) {
            this.name = PsInfoFFM.bytesToString(info.pr_oldname);
        }
        return true;
    }
}
