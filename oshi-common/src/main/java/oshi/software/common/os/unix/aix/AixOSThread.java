/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixLwpsInfo;
import oshi.driver.common.unix.aix.PsInfo;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;

/**
 * AIX OSThread implementation. Reads {@code /proc/<pid>/lwp/<tid>/lwpsinfo} via {@link PsInfo} so it has no native
 * dependency and is shared by the JNA and FFM backends.
 */
@ThreadSafe
public class AixOSThread extends AbstractOSThread {

    /**
     * Constructs a new {@code AixOSThread}.
     *
     * @param pid the process ID
     * @param tid the thread ID
     */
    public AixOSThread(int pid, int tid) {
        super(pid);
        this.threadId = tid;
        updateAttributes();
    }

    @Override
    public synchronized boolean updateAttributes() {
        AixLwpsInfo lwpsinfo = PsInfo.queryLwpsInfo(getOwningProcessId(), getThreadId());
        if (lwpsinfo == null) {
            this.state = OSProcess.State.INVALID;
            return false;
        }
        this.threadId = (int) lwpsinfo.pr_lwpid; // 64 bit storage but always 32 bit
        this.startMemoryAddress = lwpsinfo.pr_addr;
        this.state = AixProcessState.getStateFromOutput((char) lwpsinfo.pr_sname);
        this.priority = lwpsinfo.pr_pri;
        return true;
    }
}
