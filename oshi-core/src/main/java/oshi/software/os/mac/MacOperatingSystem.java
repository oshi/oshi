/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.mac;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.mac.SystemB.ProcTaskAllInfo;
import oshi.jna.platform.mac.SystemB.ProcTaskInfo;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.OSProcess;
import oshi.util.platform.mac.SysctlUtil;

public class MacOperatingSystem extends AbstractOperatingSystem {

    private static final long serialVersionUID = 1L;

    private int maxProc = 1024;

    public MacOperatingSystem() {
        this.manufacturer = "Apple";
        this.family = System.getProperty("os.name");
        this.version = new MacOSVersionInfoEx();
        // Set max processes
        this.maxProc = SysctlUtil.sysctl("kern.maxproc", 0x1000);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return new MacFileSystem();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort) {
        List<OSProcess> procs = new ArrayList<>();
        int[] pids = new int[this.maxProc];
        int numberOfProcesses = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, pids, pids.length)
                / SystemB.INT_SIZE;
        for (int i = 0; i < numberOfProcesses; i++) {
            OSProcess proc = getProcess(pids[i]);
            if (proc != null) {
                procs.add(proc);
            }
        }
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[sorted.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess getProcess(int pid) {
        ProcTaskAllInfo taskAllInfo = new ProcTaskAllInfo();
        if (0 > SystemB.INSTANCE.proc_pidinfo(pid, SystemB.PROC_PIDTASKALLINFO, 0, taskAllInfo, taskAllInfo.size())) {
            return null;
        }
        String name = null;
        String path = "";
        Pointer buf = new Memory(SystemB.PROC_PIDPATHINFO_MAXSIZE);
        if (0 < SystemB.INSTANCE.proc_pidpath(pid, buf, SystemB.PROC_PIDPATHINFO_MAXSIZE)) {
            path = buf.getString(0).trim();
            // Overwrite name with last part of path
            String[] pathSplit = path.split("/");
            if (pathSplit.length > 0) {
                name = pathSplit[pathSplit.length - 1];
            }
        }
        // If process is gone, return null
        if (taskAllInfo.ptinfo.pti_threadnum < 1) {
            return null;
        }
        if (name == null) {
            // pbi_comm contains first 16 characters of name
            // null terminated
            for (int t = 0; t < taskAllInfo.pbsd.pbi_comm.length; t++) {
                if (taskAllInfo.pbsd.pbi_comm[t] == 0) {
                    name = new String(taskAllInfo.pbsd.pbi_comm, 0, t);
                    break;
                }
            }
        }
        return new MacProcess(name, path, taskAllInfo.pbsd.pbi_status, pid, taskAllInfo.pbsd.pbi_ppid,
                taskAllInfo.ptinfo.pti_threadnum, taskAllInfo.ptinfo.pti_priority, taskAllInfo.ptinfo.pti_virtual_size,
                taskAllInfo.ptinfo.pti_resident_size, taskAllInfo.ptinfo.pti_total_system / 1000000L,
                taskAllInfo.ptinfo.pti_total_user / 1000000L,
                taskAllInfo.pbsd.pbi_start_tvsec * 1000L + taskAllInfo.pbsd.pbi_start_tvusec / 1000L,
                System.currentTimeMillis());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessId() {
        return SystemB.INSTANCE.getpid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessCount() {
        return SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, null, 0) / SystemB.INT_SIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCount() {
        // Get current pids, then slightly pad in case new process starts while
        // allocating array space
        int[] pids = new int[getProcessCount() + 10];
        int numberOfProcesses = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, pids, pids.length)
                / SystemB.INT_SIZE;
        int numberOfThreads = 0;
        ProcTaskInfo taskInfo = new ProcTaskInfo();
        for (int i = 0; i < numberOfProcesses; i++) {
            SystemB.INSTANCE.proc_pidinfo(pids[i], SystemB.PROC_PIDTASKINFO, 0, taskInfo, taskInfo.size());
            numberOfThreads += taskInfo.pti_threadnum;
        }
        return numberOfThreads;
    }
}
