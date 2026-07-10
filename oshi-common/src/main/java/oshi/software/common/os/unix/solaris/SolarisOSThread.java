/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.driver.common.unix.solaris.PsInfo;
import oshi.driver.common.unix.solaris.SolarisLwpsInfo;
import oshi.driver.common.unix.solaris.SolarisPrUsage;
import oshi.software.common.AbstractOSThread;
import oshi.util.Util;

/**
 * Abstract base for Solaris OSThread. All attribute parsing is shared via the common {@code lwpsinfo}/{@code usage}
 * carriers; there is no platform-specific behavior, so the JNA and FFM subclasses add only their constructors.
 */
public abstract class SolarisOSThread extends AbstractOSThread {

    private final Supplier<SolarisLwpsInfo> lwpsinfo = memoize(this::queryLwpsInfo, defaultExpiration());
    private final Supplier<SolarisPrUsage> prusage = memoize(this::queryPrUsage, defaultExpiration());

    protected SolarisOSThread(int pid, int lwpid) {
        super(pid);
        this.threadId = lwpid;
        updateAttributes();
    }

    private SolarisLwpsInfo queryLwpsInfo() {
        return PsInfo.queryLwpsInfo(this.getOwningProcessId(), this.getThreadId());
    }

    private SolarisPrUsage queryPrUsage() {
        return PsInfo.queryPrUsage(this.getOwningProcessId(), this.getThreadId());
    }

    @Override
    public boolean updateAttributes() {
        SolarisLwpsInfo info = lwpsinfo.get();
        if (info == null) {
            this.state = INVALID;
            return false;
        }
        SolarisPrUsage usage = prusage.get();
        long now = System.currentTimeMillis();
        this.state = SolarisOSProcess.getStateFromOutput((char) info.pr_sname);
        this.startTime = info.pr_start.toMillis();
        // Avoid divide by zero for processes up less than a millisecond
        long elapsedTime = now - this.startTime;
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.kernelTime = 0L;
        this.userTime = info.pr_time.toMillis();
        this.startMemoryAddress = info.pr_addr;
        this.priority = info.pr_pri;
        this.contextSwitches = 0L;
        if (usage != null) {
            this.userTime = usage.pr_utime.toMillis();
            this.kernelTime = usage.pr_stime.toMillis();
            this.contextSwitches = usage.pr_ictx + usage.pr_vctx;
        }
        this.name = PsInfo.bytesToString(info.pr_name);
        if (Util.isBlank(name)) {
            this.name = PsInfo.bytesToString(info.pr_oldname);
        }
        return true;
    }
}
